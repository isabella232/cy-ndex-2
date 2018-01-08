package org.cytoscape.cyndex2.internal.rest.endpoints.impl;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.Response.Status;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.ci.CIErrorFactory;
import org.cytoscape.ci.CIExceptionFactory;
import org.cytoscape.ci.CIResponseFactory;
import org.cytoscape.ci.CIWrapping;
import org.cytoscape.ci.model.CIError;
import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.rest.HeadlessTaskMonitor;
import org.cytoscape.cyndex2.internal.rest.NdexClient;
import org.cytoscape.cyndex2.internal.rest.SimpleNetworkSummary;
import org.cytoscape.cyndex2.internal.rest.endpoints.NdexNetworkResource;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorBuilder;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorType;
import org.cytoscape.cyndex2.internal.rest.parameter.NdexImportParams;
import org.cytoscape.cyndex2.internal.rest.parameter.NdexSaveParameters;
import org.cytoscape.cyndex2.internal.rest.response.NdexBaseResponse;
import org.cytoscape.cyndex2.internal.rest.response.SummaryResponse;
import org.cytoscape.cyndex2.internal.singletons.CXInfoHolder;
import org.cytoscape.cyndex2.internal.singletons.CyObjectManager;
import org.cytoscape.cyndex2.internal.singletons.NetworkManager;
import org.cytoscape.cyndex2.internal.task.NetworkExportTask;
import org.cytoscape.cyndex2.internal.task.NetworkExportTask.NetworkExportException;
import org.cytoscape.cyndex2.internal.task.NetworkImportTask;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.work.TaskMonitor;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Permissions;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NdexNetworkResourceImpl implements NdexNetworkResource {

	private static final Logger logger = LoggerFactory.getLogger(NdexNetworkResourceImpl.class);

	private final NdexClient client;
	private final TaskMonitor tm;

//	private CxTaskFactoryManager tfManager;

	private final CyNetworkManager networkManager;
	private final CyApplicationManager appManager;


	private final CIResponseFactory ciResponseFactory;
	private final CIExceptionFactory ciExceptionFactory;
	private final CIErrorFactory ciErrorFactory;

	private final ErrorBuilder errorBuilder;

	public NdexNetworkResourceImpl(final NdexClient client, final ErrorBuilder errorBuilder,
			CyApplicationManager appManager, CyNetworkManager networkManager, /*CxTaskFactoryManager tfManager,
			TaskFactory loadNetworkTF,*/ CIResponseFactory ciResponseFactory, CIExceptionFactory ciExceptionFactory, CIErrorFactory ciErrorFactory) {

		this.client = client;
		this.ciResponseFactory = ciResponseFactory;
		this.ciErrorFactory = ciErrorFactory;
		this.ciExceptionFactory = ciExceptionFactory;
		
		this.errorBuilder = errorBuilder;

		this.networkManager = networkManager;
		this.appManager = appManager;

		this.tm = new HeadlessTaskMonitor();
//		this.tfManager = tfManager;
	}

	@Override
	@CIWrapping
	public CINdexBaseResponse createNetworkFromNdex(final NdexImportParams params) {
		if (params.serverUrl == null || params.uuid == null){
			final String message = "Must provide a serverUrl and uuid to import a network";
			logger.error(message);
			throw errorBuilder.buildException(Status.BAD_REQUEST, message, ErrorType.INVALID_PARAMETERS);
		}
		NetworkImportTask importer;
		try {
			if (params.username != null && params.password != null)
				importer = new NetworkImportTask(params.username, params.password, params.serverUrl, UUID.fromString(params.uuid), params.accessKey);
			else
				importer = new NetworkImportTask(params.serverUrl, UUID.fromString(params.uuid), params.accessKey);
			importer.run(tm);
		} catch (IOException | NdexException e2) {
			final String message = "Failed to connect to server and retrieve network. " + e2.getMessage();
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);

		} catch (Exception e) {
			final String message = "Unable to create CyNetwork from NDEx." + e.getMessage();
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);

		}

		final NdexBaseResponse response = new NdexBaseResponse(importer.getSUID(), params.uuid);
		try {
			return ciResponseFactory.getCIResponse(response, CINdexBaseResponse.class);
		} catch (InstantiationException | IllegalAccessException e) {
			final String message = "Could not create wrapped CI JSON. Error: " + e.getMessage();
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}

	@Override
	@CIWrapping
	public CINdexBaseResponse saveNetworkToNdex(final Long suid, final NdexSaveParameters params) {
		if (params.isPublic == null)
			params.isPublic = Boolean.TRUE;
		if (params.metadata == null)
			params.metadata = new HashMap<>();
		
		CyNetwork network = CyObjectManager.INSTANCE.getNetworkManager().getNetwork(suid.longValue());
		
		if (network == null) {
			//Check if the suid points to a collection
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
			// Current network is not available
			final String message = "Network/Collection with SUID " + String.valueOf(suid) + " does not exist.";
			logger.error(message);
			final CIError ciError = ciErrorFactory.getCIError(Status.BAD_REQUEST.getStatusCode(),
					"urn:cytoscape:ci:ndex:v1:errors:1", message, URI.create("file:///log"));
			throw ciExceptionFactory.getCIException(Status.BAD_REQUEST.getStatusCode(), new CIError[] { ciError });
		}
		
		// Save non-null metadata to network/collection table
		if (params.metadata != null) {
			for (String column : params.metadata.keySet()) {
				saveMetadata(column, params.metadata.get(column), network);
			}
		}
		
		try {
			NetworkExportTask exporter = new NetworkExportTask(network, params, false);
			exporter.run(tm);
			String newUUID = exporter.getNetworkUUID().toString();

			if (params.isPublic == Boolean.TRUE) {
				setVisibility(params, newUUID);
			}

			final NdexBaseResponse response = new NdexBaseResponse(suid, newUUID);
			return ciResponseFactory.getCIResponse(response, CINdexBaseResponse.class);
		} catch (NetworkExportException e1) {
			final String message = "Error exporting network to CX:" + e1.getMessage();
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		} catch (InstantiationException | IllegalAccessException e2) {
			final String message = "Could not create wrapped CI JSON response. Error: " +e2.getMessage();
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		} catch(NullPointerException e){
			logger.error(e.getMessage());
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, "NULL EXCEPTION: " + e.getMessage(), ErrorType.INTERNAL);
		} catch (IOException | NdexException e) {
			logger.error(e.getMessage());
			final String message = "Unable to connect to the NDEx Java Client.";
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}

	}

	@Override
	@CIWrapping
	public CINdexBaseResponse saveCurrentNetworkToNdex(NdexSaveParameters params) {

		final CyNetwork network = appManager.getCurrentNetwork();
		if (network == null) {
			// Current network is not available
			final String message = "Current network does not exist.  You need to choose a network first.";
			logger.error(message);
			final CIError ciError = ciErrorFactory.getCIError(Status.BAD_REQUEST.getStatusCode(),
					"urn:cytoscape:ci:ndex:v1:errors:1", message, URI.create("file:///log"));
			throw ciExceptionFactory.getCIException(Status.BAD_REQUEST.getStatusCode(), new CIError[] { ciError });
		}

		return saveNetworkToNdex(network.getSUID(), params);
	}

	private final void setVisibility(final NdexSaveParameters params, final String uuid) {
		int retries = 0;
		for (; retries < 5; retries++) {
			try {
				client.setVisibility(params.serverUrl, uuid, params.isPublic.booleanValue(), params.username, params.password);
				break;
			} catch (Exception e) {
				String message = String.format("Error updating visibility. Retrying (%d/5)...", retries);
				logger.warn(message);
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e1) {
					message = "Failed to wait. This should never happen.";
					logger.error(message);
					final CIError ciError = ciErrorFactory.getCIError(Status.BAD_REQUEST.getStatusCode(),
							"urn:cytoscape:ci:ndex:v1:errors:1", message);
					throw ciExceptionFactory.getCIException(Status.BAD_REQUEST.getStatusCode(),
							new CIError[] { ciError });

				}
			}
			if (retries >= 5) {
				final String message = "NDEx appears to be busy.\n"
						+ "Your network will likely be saved in your account, but will remain private. \n"
						+ "You can use the NDEx web site to make your network public once NDEx posts it there.";
				logger.warn(message);
				throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
			}
		}

	}

	private final static void saveMetadata(String columnName, String value, CyNetwork network) {

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
		final CyNetwork network = appManager.getCurrentNetwork();

		if (network == null) {
			final String message = "Current network does not exist (No network is selected)";
			logger.error(message);
			throw errorBuilder.buildException(Status.BAD_REQUEST, message, ErrorType.INTERNAL);
		}

		final CyRootNetwork root = ((CySubNetwork) network).getRootNetwork();

		final SummaryResponse response = buildSummary(root, (CySubNetwork) network);
		try {
			return ciResponseFactory.getCIResponse(response, CISummaryResponse.class);
		} catch (InstantiationException | IllegalAccessException e) {
			final String message = "Could not create wrapped CI JSON. Error: " + e.getMessage();
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}
	
	@CIWrapping
	@Override
	public CISummaryResponse getNetworkSummary(Long suid) {
		CyNetwork network = CyObjectManager.INSTANCE.getNetworkManager().getNetwork(suid.longValue());
		CyRootNetwork rootNetwork = null;
		if (network == null) {
			//Check if the suid points to a collection
			for (CyNetwork net : CyObjectManager.INSTANCE.getNetworkManager().getNetworkSet()) {
				CyRootNetwork root = ((CySubNetwork) net).getRootNetwork();
				Long rootSUID = root.getSUID();
				if (rootSUID.compareTo(suid) == 0) {
					rootNetwork = root;
					break;
				}
			}
		}else{
			rootNetwork = ((CySubNetwork) network).getRootNetwork();
		}
		
		if (rootNetwork == null) {
			// Current network is not available
			final String message = "Cannot find collection/network with SUID " + String.valueOf(suid) + ".";
			logger.error(message);
			final CIError ciError = ciErrorFactory.getCIError(Status.BAD_REQUEST.getStatusCode(),
					"urn:cytoscape:ci:ndex:v1:errors:1", message, URI.create("file:///log"));
			throw ciExceptionFactory.getCIException(Status.BAD_REQUEST.getStatusCode(), new CIError[] { ciError });
		}

		final SummaryResponse response = buildSummary(rootNetwork, (CySubNetwork) network);
		try {
			return ciResponseFactory.getCIResponse(response, CISummaryResponse.class);
		} catch (InstantiationException | IllegalAccessException e) {
			final String message = "Could not create wrapped CI JSON. Error: " + e.getMessage();
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}

	private final static SummaryResponse buildSummary(final CyRootNetwork root, final CySubNetwork network) {
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

		UUID uuid = NetworkManager.INSTANCE.getNdexNetworkId(summary.suid);
		if (uuid != null)
			summary.uuid = uuid.toString();

		final Collection<CyColumn> columns = table.getColumns();
		final Map<String, Object> props = new HashMap<>();

		columns.stream().forEach(col -> props.put(col.getName(), row.get(col.getName(), col.getType())));
		summary.props = props;

		return summary;
	}

	@Override
	@CIWrapping
	public CINdexBaseResponse updateNetworkInNdex(Long suid, NdexSaveParameters params) {

		if (suid == null) {
			logger.error("SUID is missing");
			throw errorBuilder.buildException(Status.BAD_REQUEST, "SUID is not specified.",
					ErrorType.INVALID_PARAMETERS);
		}

		CyNetwork network = networkManager.getNetwork(suid.longValue());
		
		if (network == null) {
			//Check if the suid points to a collection
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
			final String message = "Network with SUID " + suid + " does not exist.";
			logger.error(message);
			throw errorBuilder.buildException(Status.NOT_FOUND, message, ErrorType.INVALID_PARAMETERS);
		}

		// Check UUID
		UUID uuid;
		try {
			uuid = updateIsPossibleHelper(suid, params);
		} catch (Exception e) {
			final String message = "Unable to update network in NDEx." + e.getMessage()
					+ " Try saving as a new network.";
			logger.error(message);
			throw errorBuilder.buildException(Status.BAD_REQUEST, message, ErrorType.INVALID_PARAMETERS);

		}

		if (params.metadata != null) {
			for (String key : params.metadata.keySet()) {
				saveMetadata(key, params.metadata.get(key), network);
			}
		}
	//	final CyNetworkViewWriterFactory writerFactory = tfManager.getCxWriterFactory();

		int retryCount = 0;
		boolean success = false;
		while (retryCount <= 3) {
			try {
				// takes a subnetwork
				success = updateExistingNetwork(/*writerFactory,*/ network, params/*, uuid*/);
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
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}

		final String uuidStr = uuid.toString();
		// Visibility
		setVisibility(params, uuidStr);

		final NdexBaseResponse response = new NdexBaseResponse(suid, uuidStr);
		try {
			return ciResponseFactory.getCIResponse(response, CINdexBaseResponse.class);
		} catch (InstantiationException | IllegalAccessException e) {
			final String message = "Could not create wrapped CI JSON. Error: "+ e.getMessage();
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}

	private final boolean updateExistingNetwork(/*final CyNetworkViewWriterFactory writerFactory,*/ final CyNetwork network,
			final NdexSaveParameters params /*, final UUID uuid*/) {

		
		try {
			NetworkExportTask updater = new NetworkExportTask(network, params, true);
			updater.run(tm);
		} catch (NetworkExportException e) {
			logger.error(e.getMessage());
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, e.getMessage(), ErrorType.INTERNAL);
		} catch (IOException | NdexException e) {
			logger.error(e.getMessage());
			final String message = "Unable to connect to the NDEx Java Client.";
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
		return true;
	}

	@Override
	@CIWrapping
	public CINdexBaseResponse updateCurrentNetworkInNdex(NdexSaveParameters params) {
		final CyNetwork network = appManager.getCurrentNetwork();
		if (network == null) {
			final String message = "Current network does not exist (No network is selected)";
			logger.error(message);
			throw errorBuilder.buildException(Status.BAD_REQUEST, message, ErrorType.INTERNAL);
		}
		return updateNetworkInNdex(network.getSUID(), params);
	}

	private static UUID updateIsPossibleHelper(final Long suid, final NdexSaveParameters params) throws Exception {

		UUID ndexNetworkId = null;
		CXInfoHolder cxInfo = NetworkManager.INSTANCE.getCXInfoHolder(suid);
		if (cxInfo != null)
			ndexNetworkId = cxInfo.getNetworkId();
		if (ndexNetworkId == null)
			ndexNetworkId = NetworkManager.INSTANCE.getNdexNetworkId(suid);

		if (ndexNetworkId == null) {
			throw new Exception(
					"NDEx network UUID not found. You can only update networks that were imported with CyNDEx2");
		}

		final NdexRestClient nc = new NdexRestClient(params.username, params.password, params.serverUrl,
				CyActivator.getAppName()+"("+CyActivator.getAppVersion()+")");
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

}
