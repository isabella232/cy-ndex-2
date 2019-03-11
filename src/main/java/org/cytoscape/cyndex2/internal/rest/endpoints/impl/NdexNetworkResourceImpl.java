package org.cytoscape.cyndex2.internal.rest.endpoints.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.SwingUtilities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.ci.CIWrapping;
import org.cytoscape.ci.model.CIError;
import org.cytoscape.cyndex2.internal.CxTaskFactoryManager;
import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.CyServiceModule;
import org.cytoscape.cyndex2.internal.rest.NdexClient;
import org.cytoscape.cyndex2.internal.rest.SimpleNetworkSummary;
import org.cytoscape.cyndex2.internal.rest.endpoints.NdexNetworkResource;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorBuilder;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorType;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExBasicSaveParameters;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExImportParameters;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExSaveParameters;
import org.cytoscape.cyndex2.internal.rest.response.NdexBaseResponse;
import org.cytoscape.cyndex2.internal.rest.response.SummaryResponse;
import org.cytoscape.cyndex2.internal.task.NDExExportTaskFactory;
import org.cytoscape.cyndex2.internal.task.NDExImportTaskFactory;
import org.cytoscape.cyndex2.internal.util.CIServiceManager;
import org.cytoscape.cyndex2.internal.util.NetworkUUIDManager;
import org.cytoscape.io.read.AbstractCyNetworkReader;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;
import org.cytoscape.work.swing.DialogTaskManager;
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

	private final CyNetworkManager networkManager;
	private final CyApplicationManager appManager;
	private final CIServiceManager ciServiceManager;

	private final ErrorBuilder errorBuilder;

	public NdexNetworkResourceImpl(final NdexClient client, final ErrorBuilder errorBuilder,
			CyApplicationManager appManager, CyNetworkManager networkManager, CIServiceManager ciServiceTracker) {

		this.client = client;
		this.ciServiceManager = ciServiceTracker;

		this.errorBuilder = errorBuilder;

		this.networkManager = networkManager;
		this.appManager = appManager;

		// this.tfManager = tfManager;
	}

	private CyNetwork getCurrentNetwork() {
		CyNetwork network = appManager.getCurrentNetwork();
		if (network == null) {
			final String message = "Current network does not exist. Select a network or specify an SUID.";
			throw errorBuilder.buildException(Status.BAD_REQUEST, message, ErrorType.INVALID_PARAMETERS);
		}
		return network;
	}
	
	private CyNetwork getNetworkFromSUID(Long suid) throws WebApplicationException {
		/*
		 * Attempt to get the CyNetwork object from an SUID. If the SUID is null, get
		 * the currently selected CySubNetwork. An SUID may specify a subnetwork or a
		 * collection object in Cytoscape.
		 * 
		 * If there is not network with the given SUID, or the current network is null,
		 * throw a WebApplicationException. This function will not return null
		 */
		if (suid == null) {
			logger.error("SUID is missing");
			throw errorBuilder.buildException(Status.BAD_REQUEST, "SUID is not specified.",
					ErrorType.INVALID_PARAMETERS);
		}
		CyNetwork network = networkManager.getNetwork(suid.longValue());

		if (network == null) {
			// Check if the suid points to a collection
			for (CyNetwork net : networkManager.getNetworkSet()) {
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

	@Override
	@CIWrapping
	public CINdexBaseResponse createNetworkFromNdex(final NDExImportParameters params) {
		
		try {
			NDExImportTaskFactory importFactory = new NDExImportTaskFactory(params, errorBuilder);
			TaskIterator iter = importFactory.createTaskIterator();
			
			execute(iter);
			
			final NdexBaseResponse response = new NdexBaseResponse(importFactory.getSUID(), params.uuid);
		
			return ciServiceManager.getCIResponseFactory().getCIResponse(response, CINdexBaseResponse.class);
		} catch (InstantiationException | IllegalAccessException e) {
			final String message = "Could not create wrapped CI JSON. Error: " + e.getMessage();
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}

	@Override
	@CIWrapping
	public CINdexBaseResponse saveNetworkToNdex(final Long suid, final NDExSaveParameters params) {
		try {
			NDExExportTaskFactory exportFactory = new NDExExportTaskFactory(params, errorBuilder, false);
			CyNetwork network = getNetworkFromSUID(suid);
			
			TaskIterator iter = exportFactory.createTaskIterator(network);
			
			execute(iter);
			
			UUID newUUID = exportFactory.getUUID();
			if (newUUID == null) {
				final String message = "No UUID returned from NDEx API.";
				logger.error(message);
				throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
			}
			setVisibility(params, newUUID.toString());
			
			final NdexBaseResponse response = new NdexBaseResponse(suid, newUUID.toString());
			return ciServiceManager.getCIResponseFactory().getCIResponse(response, CINdexBaseResponse.class);
		
		} catch (InstantiationException | IllegalAccessException e2) {
			final String message = "Could not create wrapped CI JSON response. Error: " + e2.getMessage();
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
		
	}

	@Override
	@CIWrapping
	public CINdexBaseResponse saveCurrentNetworkToNdex(NDExSaveParameters params) {
		final CyNetwork network = getCurrentNetwork();
		return saveNetworkToNdex(network.getSUID(), params);
	}

	private final void setVisibility(final NDExSaveParameters params, final String uuid) {
		int retries = 0;
		for (; retries < 5; retries++) {
			try {
				client.setVisibility(params.serverUrl, uuid, params.isPublic.booleanValue(), params.username,
						params.password);
				break;
			} catch (Exception e) {
				String message = String.format("Error updating visibility. Retrying (%d/5)...", retries);
				logger.warn(message);
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e1) {
					message = "Failed to wait. This should never happen.";
					logger.error(message);
					final CIError ciError = ciServiceManager.getCIErrorFactory().getCIError(
							Status.BAD_REQUEST.getStatusCode(), "urn:cytoscape:ci:ndex:v1:errors:1", message);
					throw ciServiceManager.getCIExceptionFactory().getCIException(Status.BAD_REQUEST.getStatusCode(),
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

	@CIWrapping
	@Override
	public CISummaryResponse getCurrentNetworkSummary() {
		final CyNetwork network = getCurrentNetwork();
		final CyRootNetwork root = ((CySubNetwork) network).getRootNetwork();
		final SummaryResponse response = buildSummary(root, (CySubNetwork) network);
		try {
			return ciServiceManager.getCIResponseFactory().getCIResponse(response, CISummaryResponse.class);
		} catch (InstantiationException | IllegalAccessException e) {
			final String message = "Could not create wrapped CI JSON. Error: " + e.getMessage();
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}

	@CIWrapping
	@Override
	public CISummaryResponse getNetworkSummary(Long suid) {
		CyNetwork network = networkManager.getNetwork(suid.longValue());
		CyRootNetwork rootNetwork = null;
		if (network == null) {
			// Check if the suid points to a collection
			for (CyNetwork net : networkManager.getNetworkSet()) {
				CyRootNetwork root = ((CySubNetwork) net).getRootNetwork();
				Long rootSUID = root.getSUID();
				if (rootSUID.compareTo(suid) == 0) {
					rootNetwork = root;
					break;
				}
			}
		} else {
			rootNetwork = ((CySubNetwork) network).getRootNetwork();
		}

		if (rootNetwork == null) {
			// Current network is not available
			final String message = "Cannot find collection/network with SUID " + String.valueOf(suid) + ".";
			logger.error(message);
			final CIError ciError = ciServiceManager.getCIErrorFactory().getCIError(Status.BAD_REQUEST.getStatusCode(),
					"urn:cytoscape:ci:ndex:v1:errors:1", message, URI.create("file:///log"));
			throw ciServiceManager.getCIExceptionFactory().getCIException(Status.BAD_REQUEST.getStatusCode(),
					new CIError[] { ciError });
		}

		final SummaryResponse response = buildSummary(rootNetwork, (CySubNetwork) network);
		try {
			return ciServiceManager.getCIResponseFactory().getCIResponse(response, CISummaryResponse.class);
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

		UUID uuid = NetworkUUIDManager.getUUID(network);
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
	public CINdexBaseResponse updateNetworkInNdex(Long suid, NDExBasicSaveParameters params) {
		
		CyNetwork network = getNetworkFromSUID(suid);
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
		
		boolean success = updateLoop(network, params);

		if (!success) {
			final String message = "Could not update existing NDEx entry.  NDEx server did not accept your request.";
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}

		final String uuidStr = uuid.toString();

		final NdexBaseResponse response = new NdexBaseResponse(suid, uuidStr);
		try {
			return ciServiceManager.getCIResponseFactory().getCIResponse(response, CINdexBaseResponse.class);
		} catch (InstantiationException | IllegalAccessException e) {
			final String message = "Could not create wrapped CI JSON. Error: " + e.getMessage();
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}

	private final boolean updateExistingNetwork(
			final CyNetwork network,
			final NDExBasicSaveParameters params) {

		NDExExportTaskFactory exportFactory = new NDExExportTaskFactory(params, errorBuilder, true);
		TaskIterator iter = exportFactory.createTaskIterator(network);
		CyActivator.taskManager.execute(iter);
		return true;
	}
	
	private boolean updateLoop(CyNetwork network, NDExBasicSaveParameters params) {
		int retryCount = 0;
		boolean success = false;
		while (retryCount <= 3) {
			try {
				// takes a subnetwork
				success = updateExistingNetwork(network, params);
				if (success) {
					return true;
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
		return false;
	}

	@Override
	@CIWrapping
	public CINdexBaseResponse updateCurrentNetworkInNdex(NDExBasicSaveParameters params) {
		final CyNetwork network = getCurrentNetwork();
		return updateNetworkInNdex(network.getSUID(), params);
	}

	private static UUID updateIsPossibleHelper(final Long suid, final NDExBasicSaveParameters params) throws Exception {

		CyNetworkManager network_manager = CyServiceModule.getService(CyNetworkManager.class);
		CyNetwork network = network_manager.getNetwork(suid);
		UUID ndexNetworkId = NetworkUUIDManager.getUUID(network);
		
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

		InputStreamTaskFactory taskFactory = CxTaskFactoryManager.INSTANCE.getCxReaderFactory();
		TaskIterator iter = taskFactory.createTaskIterator(in, null);
		
		// Get task to get SUID
		AbstractCyNetworkReader reader = (AbstractCyNetworkReader) iter.next();
		iter.append(reader);
		
		execute(iter);
		
		for (CyNetwork net : reader.getNetworks()) {
			networkManager.addNetwork(net);
		}
		reader.buildCyNetworkView(reader.getNetworks()[0]);
		
		Long suid = reader.getNetworks()[0].getSUID();
		final NdexBaseResponse response = new NdexBaseResponse(suid, "");
		try {
			return ciServiceManager.getCIResponseFactory().getCIResponse(response, CINdexBaseResponse.class);
		} catch (InstantiationException | IllegalAccessException e) {
			final String message = "Could not create wrapped CI JSON. Error: " + e.getMessage();
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}
	
	private void execute(TaskIterator iter) {
		DialogTaskManager tm = CyServiceModule.getService(DialogTaskManager.class);
//		SynchronousTaskManager<?> tm = CyServiceModule.getService(SynchronousTaskManager.class);
		
		Object lock = new Object();
		Runnable runner = new Runnable() {
			@Override
			public void run() {
				tm.execute(iter, new TaskObserver() {
					
					@Override
					public void taskFinished(ObservableTask task) {
						
					}
					
					@Override
					public void allFinished(FinishStatus finishStatus) {
						synchronized(lock) {
							lock.notify();
						}
					}
				});
			}
		};

		try {
            SwingUtilities.invokeAndWait(runner);
            synchronized (lock) {
	            lock.wait();	
			}
        }
        catch (Exception e) {
            e.printStackTrace();
        }
	}

}
