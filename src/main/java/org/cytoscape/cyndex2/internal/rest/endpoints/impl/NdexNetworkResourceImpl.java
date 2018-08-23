package org.cytoscape.cyndex2.internal.rest.endpoints.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.ci.CIWrapping;
import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.rest.NdexClient;
import org.cytoscape.cyndex2.internal.rest.SimpleNetworkSummary;
import org.cytoscape.cyndex2.internal.rest.endpoints.NdexNetworkResource;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorBuilder;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorType;
import org.cytoscape.cyndex2.internal.rest.parameter.NdexBasicSaveParameter;
import org.cytoscape.cyndex2.internal.rest.parameter.NdexImportParams;
import org.cytoscape.cyndex2.internal.rest.parameter.NdexSaveParameters;
import org.cytoscape.cyndex2.internal.rest.response.NdexBaseResponse;
import org.cytoscape.cyndex2.internal.rest.response.SummaryResponse;
import org.cytoscape.cyndex2.internal.singletons.CXInfoHolder;
import org.cytoscape.cyndex2.internal.singletons.CyObjectManager;
import org.cytoscape.cyndex2.internal.task.NetworkExportTask;
import org.cytoscape.cyndex2.internal.task.NetworkImportTask;
import org.cytoscape.cyndex2.internal.util.CIServiceManager;
import org.cytoscape.cyndex2.internal.util.HeadlessTaskMonitor;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Permissions;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

import com.fasterxml.jackson.core.JsonProcessingException;

public class NdexNetworkResourceImpl implements NdexNetworkResource {

	private final NdexClient client;

	private final CyApplicationManager appManager;
	private final CIServiceManager ciServiceManager;

	private final ErrorBuilder errorBuilder;

	public NdexNetworkResourceImpl(final NdexClient client, final ErrorBuilder errorBuilder,
			CyApplicationManager appManager, CIServiceManager ciServiceTracker) {

		this.client = client;
		this.ciServiceManager = ciServiceTracker;

		this.errorBuilder = errorBuilder;

		this.appManager = appManager;
	}

	private CyNetwork getNetworkFromSUID(Long suid) throws WebApplicationException{
		/*
		 * Attempt to get the CyNetwork object from an SUID. If the SUID is null, get the currently selected CySubNetwork.
		 * An SUID may specify a subnetwork or a collection object in Cytoscape.
		 * 
		 * If there is not network with the given SUID, or the current network is null, throw a WebApplicationException.
		 * This function will not return null
		 */
		if (suid == null) {
			CyNetwork network = appManager.getCurrentNetwork();
			if (network == null) {
				final String message = "Current network does not exist. Select a network or specify an SUID.";
				throw errorBuilder.buildException(Status.BAD_REQUEST, message, ErrorType.INVALID_PARAMETERS);
			}
			return network;
		}
		
		CyNetwork network = CyObjectManager.INSTANCE.getNetworkManager().getNetwork(suid.longValue());
	
		if (network == null) {
			// Check if the suid points to a collection
			for (CyNetwork net : CyObjectManager.INSTANCE.getNetworkManager().getNetworkSet()) {
				CyRootNetwork root = ((CySubNetwork) net).getRootNetwork();
				Long rootSUID = root.getSUID();
				if (rootSUID.compareTo(suid) == 0) {
					network = root;
					break;
				}
			}
		}
		if (network == null) {
			// Network is not available
			final String message = "Network/Collection with SUID " + String.valueOf(suid) + " does not exist.";
			throw errorBuilder.buildException(Status.BAD_REQUEST, message, ErrorType.INVALID_PARAMETERS);
		}
		return network;
	}

	private NetworkExportTask buildExportTask(Long suid, NdexSaveParameters params) throws JsonProcessingException, IOException, NdexException {
		/*
		 * Verify export parameters and create the NetworkExportTask object.
		 * Default parameters are:
		 * - isPublic -> true
		 * - metadata -> empty map
		 * 
		 * Metadata is saved to the network before creating the export task
		 * 
		 */
		validateSaveParameters(params);

		CyNetwork network = getNetworkFromSUID(suid);
		
		for (String column : params.metadata.keySet()) {
			saveMetadata(column, params.metadata.get(column), network);
		}
		
		return new NetworkExportTask(network, params, false);
	}
	
	private void validateImportParameters(NdexImportParams params) {
		if (params == null) {
			final String message = "No import parameters found.";
			throw errorBuilder.buildException(Status.BAD_REQUEST, message, ErrorType.INVALID_PARAMETERS);
		}
		if (params.serverUrl == null) {
			params.serverUrl = "http://ndexbio.org/v2";
		}
		if (params.uuid == null) {
			final String message = "Must provide a uuid to import a network";
			throw errorBuilder.buildException(Status.BAD_REQUEST, message, ErrorType.INVALID_PARAMETERS);
		}
	}

	private NetworkImportTask buildImportTask(NdexImportParams params) {
		/*
		 * Build the NetworkImportTask from the import parameters
		 * Default parameters are:
		 * - serverUrl -> "http://ndexbio.org/v2"
		 * 
		 * Attempt to create an NdexImportTask with the given network access parameters
		 */
		validateImportParameters(params);
		
		try {
			UUID uuid = UUID.fromString(params.uuid);
			if (params.username != null && params.password != null)
				return NetworkImportTask.withLogin(params.username, params.password, params.serverUrl, uuid,
						params.accessKey);
			else {
				return NetworkImportTask.withIdToken(params.serverUrl, uuid, params.accessKey, params.idToken);
			}
		} catch (IllegalArgumentException e) {
			final String message = "Provided UUID is invalid";
			throw errorBuilder.buildException(Status.BAD_REQUEST, message, ErrorType.INVALID_PARAMETERS);
		} catch (IOException | NdexException e) {
			final String message = "Failed to connect to server and retrieve network. " + e.getMessage();
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}

	}

	@Override
	@CIWrapping
	public CINdexBaseResponse createNetworkFromNdex(final NdexImportParams params) {
		NetworkImportTask importer = buildImportTask(params);

		final NdexBaseResponse response = waitForResults(importer, true);
		try {
			return ciServiceManager.getCIResponseFactory().getCIResponse(response, CINdexBaseResponse.class);
		} catch (InstantiationException | IllegalAccessException e) {
			final String message = "Could not create wrapped CI JSON. Error: " + e.getMessage();
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}

	@Override
	@CIWrapping
	public CINdexBaseResponse saveNetworkToNdex(final Long suid, NdexSaveParameters params) {
		try {
			NetworkExportTask exporter = buildExportTask(suid, params);
			final NdexBaseResponse response = waitForResults(exporter, true);
			
			if (response == null) {
				throw new NullPointerException("Response is empty. Check the local log for task errors.");
			}
			if (params.isPublic == Boolean.TRUE) {
				setVisibility(params, response.uuid);
			}
			return ciServiceManager.getCIResponseFactory().getCIResponse(response, CINdexBaseResponse.class);

		} catch (InstantiationException | IllegalAccessException e2) {
			final String message = "Could not create wrapped CI JSON response. Error: " + e2.getMessage();
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		} catch (NullPointerException e) {
			e.printStackTrace();
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, "NPE saving network " + suid + " : " + e.getLocalizedMessage(),
					ErrorType.INTERNAL);
		} catch (IOException | NdexException e) {
			final String message = "Unable to connect to the NDEx Java Client.";
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}

	@Override
	@CIWrapping
	public CINdexBaseResponse saveCurrentNetworkToNdex(NdexSaveParameters params) {
		final CyNetwork network = getNetworkFromSUID(null);
		return saveNetworkToNdex(network.getSUID(), params);
	}

	private final void setVisibility(final NdexSaveParameters params, final String uuid) {
		int retries = 0;
		for (; retries < 5; retries++) {
			try {
				client.setVisibility(params.serverUrl, uuid, params.isPublic.booleanValue(), params.username,
						params.password);
				break;
			} catch (Exception e) {
				String message = String.format("Error updating visibility. Retrying (%d/5)...", retries);
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e1) {
					message = "Failed to wait. This should never happen.";
					throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
				}
			}
			if (retries >= 5) {
				final String message = "NDEx appears to be busy.\n"
						+ "Your network will likely be saved in your account, but will remain private. \n"
						+ "You can use the NDEx web site to make your network public once NDEx posts it there.";
				throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.NDEX_API);
			}
		}

	}

	private final static void saveMetadata(String columnName, String value, CyNetwork network) {
		/*
		 * Helper function to add metadata data to the network table of a network
		 * This should be removed as data should be managed elsewhere. CyRootNetworks should not handle metadata
		 */

		final CyTable localTable = network.getTable(CyNetwork.class, CyNetwork.LOCAL_ATTRS);
		final CyRow row = localTable.getRow(network.getSUID());

		// Create new column if it does not exist
		final CyColumn col = localTable.getColumn(columnName);
		if (col == null) {
			if (value == null || value.isEmpty())
				return;
			localTable.createColumn(columnName, String.class, false);
		}

		// Set the value to local table
		row.set(columnName, value);
	}

	@CIWrapping
	@Override
	public CISummaryResponse getCurrentNetworkSummary() {
		return getNetworkSummary(null);
	}

	@CIWrapping
	@Override
	public CISummaryResponse getNetworkSummary(Long suid) {
		CyNetwork network = getNetworkFromSUID(suid);
		CyRootNetwork rootNetwork = null;
		if (network instanceof CySubNetwork) {
			rootNetwork = ((CySubNetwork) network).getRootNetwork();
		}else if (rootNetwork instanceof CyRootNetwork){
			rootNetwork = (CyRootNetwork) network;
		}

		if (rootNetwork == null) {
			// Current network is not available
			final String message = "Cannot find collection/network with SUID " + String.valueOf(suid) + ".";
			throw errorBuilder.buildException(Status.BAD_REQUEST, message, ErrorType.INVALID_PARAMETERS);
		}

		final SummaryResponse response = buildSummary(rootNetwork, (CySubNetwork) network);
		try {
			return ciServiceManager.getCIResponseFactory().getCIResponse(response, CISummaryResponse.class);
		} catch (InstantiationException | IllegalAccessException e) {
			final String message = "Could not create wrapped CI JSON. Error: " + e.getMessage();
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}

	private final static SummaryResponse buildSummary(final CyRootNetwork root, final CySubNetwork network) {
		/*
		 * Build a SummaryResponse containing the network summary of a root collection. If a subnetwork is specified, the 
		 * currentNetworkSuid attribute is set
		 */
		final SummaryResponse summary = new SummaryResponse();

		// Network local table
		final SimpleNetworkSummary rootSummary = buildNetworkSummary(root, root.getDefaultNetworkTable(),
				root.getSUID());
		if (network != null)
			summary.currentNetworkSuid = network.getSUID();
		summary.currentRootNetwork = rootSummary;
		List<SimpleNetworkSummary> members = new ArrayList<>();
		root.getSubNetworkList().stream().forEach(
				subnet -> members.add(buildNetworkSummary(subnet, subnet.getDefaultNetworkTable(), subnet.getSUID())));
		summary.members = members;

		return summary;
	}

	private final static SimpleNetworkSummary buildNetworkSummary(CyNetwork network, CyTable table, Long networkSuid) {
		
		SimpleNetworkSummary summary = new SimpleNetworkSummary();
		CyRow row = table.getRow(networkSuid);
		summary.suid = network.getSUID();
		// Get NAME from local table because this is always local.
		summary.name = network.getTable(CyNetwork.class, CyNetwork.LOCAL_ATTRS).getRow(network.getSUID())
				.get(CyNetwork.NAME, String.class);

		UUID uuid = CXInfoHolder.getNdexNetworkId(network);
		if (uuid != null)
			summary.uuid = uuid.toString();

		final Collection<CyColumn> columns = table.getColumns();
		final Map<String, Object> props = new HashMap<>();

		columns.stream().forEach(col -> props.put(col.getName(), row.get(col.getName(), col.getType())));
		summary.props = props;

		return summary;
	}
	
	private void validateSaveParameters(final NdexBasicSaveParameter params) {
		if (params == null || params.username == null || params.password == null) {
			throw errorBuilder.buildException(Status.BAD_REQUEST, "Must provide save parameters (username and password)",
					ErrorType.INVALID_PARAMETERS);
		}
		if (params.serverUrl == null) {
			params.serverUrl = "http://ndexbio.org/v2/";
		}
		if (params.metadata == null){
			params.metadata = new HashMap<>();
		}
		if (params instanceof NdexSaveParameters) {
			((NdexSaveParameters) params).isPublic = true;
		}
	}

	@Override
	@CIWrapping
	public CINdexBaseResponse updateNetworkInNdex(Long suid, NdexBasicSaveParameter params) {

		validateSaveParameters(params);
		
		if (suid == null) {
			throw errorBuilder.buildException(Status.BAD_REQUEST, "SUID is not specified.",
					ErrorType.INVALID_PARAMETERS);
		}

		CyNetwork network = getNetworkFromSUID(suid.longValue());

		// Check UUID
		UUID uuid;
		try {
			uuid = updateIsPossibleHelper(network, params);
		} catch (Exception e) {
			final String message = "Unable to update network in NDEx." + e.getMessage()
					+ " Try saving as a new network.";
			throw errorBuilder.buildException(Status.BAD_REQUEST, message, ErrorType.INVALID_PARAMETERS);

		}

		if (params.metadata != null) {
			for (String key : params.metadata.keySet()) {
				saveMetadata(key, params.metadata.get(key), network);
			}
		}
		

		int retryCount = 0;
		boolean success = false;
		while (retryCount <= 3) {
			try {
				// takes a subnetwork
				success = updateExistingNetwork( network, params);
				if (success) {
					break;
				}
			} catch (Exception e) {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				e.printStackTrace();

			} finally {
				retryCount++;
			}
		}

		if (!success) {
			final String message = "Could not update existing NDEx entry.  NDEx server did not accept your request.";
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}

		final String uuidStr = uuid.toString();

		final NdexBaseResponse response = new NdexBaseResponse(suid, uuidStr);
		try {
			return ciServiceManager.getCIResponseFactory().getCIResponse(response, CINdexBaseResponse.class);
		} catch (InstantiationException | IllegalAccessException e) {
			final String message = "Could not create wrapped CI JSON. Error: " + e.getMessage();
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}

	private final boolean updateExistingNetwork(final CyNetwork network,
			final NdexBasicSaveParameter params) {

		
		try {
			NetworkExportTask updater = new NetworkExportTask(network, params, true);
			
			synchronized (Monitor.INSTANCE) {
				TaskIterator ti = new TaskIterator(updater);
				CyActivator.taskManager.execute(ti);
				Monitor.INSTANCE.wait();
			}
		} catch (IOException | NdexException e) {
			final String message = "Unable to connect to the NDEx Java Client.";
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	@CIWrapping
	public CINdexBaseResponse updateCurrentNetworkInNdex(NdexBasicSaveParameter params) {
		final CyNetwork network = getNetworkFromSUID(null);
		return updateNetworkInNdex(network.getSUID(), params);
	}

	private static UUID updateIsPossibleHelper(CyNetwork network, final NdexBasicSaveParameter params)
			throws Exception {
		
		UUID ndexNetworkId = null;

		ndexNetworkId = CXInfoHolder.getNdexNetworkId(network);

		if (ndexNetworkId == null) {
			throw new Exception(
					"NDEx network UUID not found. You can only update networks that were imported with CyNDEx2");
		}

		final NdexRestClient nc = new NdexRestClient(params.username, params.password, params.serverUrl,
				CyActivator.getAppName() + "/" + CyActivator.getAppVersion());
		final NdexRestClientModelAccessLayer mal = new NdexRestClientModelAccessLayer(nc);
		try {

			Map<String, Permissions> permissionTable = mal.getUserNetworkPermission(nc.getUserUid(), ndexNetworkId,
					false);
			if (permissionTable == null || permissionTable.get(ndexNetworkId.toString()) == Permissions.READ)
				throw new Exception("You don't have permission to write to this network.");

		} catch (IOException | NdexException e) {
			throw new Exception("Unable to read network permissions. " + e.getMessage());
		}

		NetworkSummary ns = null;
		try {
			ns = mal.getNetworkSummaryById(ndexNetworkId);
			if (ns.getIsReadOnly())
				throw new Exception("The network is read only.");

		} catch (IOException | NdexException e) {
			throw new Exception(" An error occurred while checking permissions. " + e.getMessage());
		}
		return ndexNetworkId;
	}

	@Override
	@CIWrapping
	public CINdexBaseResponse createNetworkFromCx(final InputStream in) {

		NetworkImportTask importer;
		try {
			importer = new NetworkImportTask(in);
		} catch (Exception e) {
			final String message = "Unable to create CyNetwork from NDEx." + e.getMessage();
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);

		}

		final NdexBaseResponse response = waitForResults(importer, false);
		try {
			return ciServiceManager.getCIResponseFactory().getCIResponse(response, CINdexBaseResponse.class);
		} catch (InstantiationException | IllegalAccessException e) {
			final String message = "Could not create wrapped CI JSON. Error: " + e.getMessage();
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}

	public static class Monitor {
		public static final Monitor INSTANCE = new Monitor();
		
	}
	
	public NdexBaseResponse waitForResults(ObservableTask ot, boolean dialog) {
		if (dialog) {
			TaskIterator ti = new TaskIterator(ot);
			CyActivator.taskManager.execute(ti);
			synchronized (Monitor.INSTANCE) {
				try {
					Monitor.INSTANCE.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			return ot.getResults(NdexBaseResponse.class);
		} else {
			try {
				ot.run(new HeadlessTaskMonitor());
			} catch (Exception e) {
				e.printStackTrace();
			}
			return ot.getResults(NdexBaseResponse.class);
		}

	}

}
