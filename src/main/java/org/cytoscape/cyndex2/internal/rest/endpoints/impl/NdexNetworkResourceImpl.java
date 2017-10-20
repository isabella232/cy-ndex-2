package org.cytoscape.cyndex2.internal.rest.endpoints.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
import org.cytoscape.ci.CIWrapping;
import org.cytoscape.ci.model.CIError;
import org.cytoscape.ci_bridge_impl.CIProvider;
import org.cytoscape.cyndex2.internal.CxTaskFactoryManager;
import org.cytoscape.cyndex2.internal.rest.HeadlessTaskMonitor;
import org.cytoscape.cyndex2.internal.rest.NdexClient;
import org.cytoscape.cyndex2.internal.rest.NetworkSummary;
import org.cytoscape.cyndex2.internal.rest.endpoints.NdexNetworkResource;
import org.cytoscape.cyndex2.internal.rest.endpoints.NdexStatusResource;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorBuilder;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorType;
import org.cytoscape.cyndex2.internal.rest.parameter.NdexImportParams;
import org.cytoscape.cyndex2.internal.rest.parameter.NdexSaveParameters;
import org.cytoscape.cyndex2.internal.rest.reader.CxReaderFactory;
import org.cytoscape.cyndex2.internal.rest.reader.UpdateTableTask;
import org.cytoscape.cyndex2.internal.rest.response.NdexBaseResponse;
import org.cytoscape.cyndex2.internal.rest.response.SummaryResponse;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.write.CyNetworkViewWriterFactory;
import org.cytoscape.io.write.CyWriter;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NdexNetworkResourceImpl implements NdexNetworkResource {

	private static final Logger logger = LoggerFactory.getLogger(NdexNetworkResourceImpl.class);

	// Subnet ID tag in NDEx netwoprk summary
	private static final String SUBNET_TAG = "subnetworkIds";
	private static final String ORIGINAL_UUID_TAG = "ndex.uuid.createdFrom";

	private final NdexClient client;
	private final TaskMonitor tm;

	private CxTaskFactoryManager tfManager;

	private final CxReaderFactory loadNetworkTF;

	private final CyNetworkManager networkManager;
	private final CyApplicationManager appManager;

	private final CIExceptionFactory ciExceptionFactory;
	private final CIErrorFactory ciErrorFactory;

	private final ErrorBuilder errorBuilder;

	public NdexNetworkResourceImpl(final NdexClient client, final ErrorBuilder errorBuilder,
			CyApplicationManager appManager, CyNetworkManager networkManager, CxTaskFactoryManager tfManager,
			TaskFactory loadNetworkTF, CIExceptionFactory ciExceptionFactory, CIErrorFactory ciErrorFactory) {

		this.client = client;
		this.ciErrorFactory = ciErrorFactory;
		this.ciExceptionFactory = ciExceptionFactory;
		this.errorBuilder = errorBuilder;

		this.networkManager = networkManager;
		this.appManager = appManager;

		this.tm = new HeadlessTaskMonitor();
		this.tfManager = tfManager;
		this.loadNetworkTF = (CxReaderFactory) loadNetworkTF;
	}

	@Override
	@CIWrapping
	public CINdexBaseResponse createNetworkFromNdex(final NdexImportParams params) {

		// 1. Get summary of the network.
		Map<String, ?> summary = null;

		summary = client.getSummary(params.serverUrl, params.uuid, params.userId, params.password);

		boolean createdFromSingleton = false;
		if (summary.keySet().contains(SUBNET_TAG)) {
			Object subnets = summary.get(SUBNET_TAG);
			if (subnets instanceof List) {
				final List<Long> subnetArray = (List<Long>) subnets;

				if (subnetArray.isEmpty()) {
					createdFromSingleton = true;
				} else {
					// This is a Cytoscape network
					createdFromSingleton = false;
				}
			}
		}

		System.out.println("created from Singleton = " + createdFromSingleton);

		// Load network from ndex
		InputStream is;
		Long newSuid = null;

		is = client.load(params.serverUrl + "/network/" + params.uuid, params.userId, params.password);

		try {
			InputStreamTaskFactory readerTF = this.tfManager.getCxReaderFactory();
			TaskIterator itr = readerTF.createTaskIterator(is, "ndexCollection");
			CyNetworkReader reader = (CyNetworkReader) itr.next();
			TaskIterator tasks = loadNetworkTF.createTaskIterator(summary.get("name").toString(), reader);

			// Update table AFTER loading
			if (!createdFromSingleton) {
				UpdateTableTask updateTableTask = new UpdateTableTask(reader);
				updateTableTask.setUuid(params.uuid);
				tasks.append(updateTableTask);
			}

			while (tasks.hasNext()) {
				final Task task = tasks.next();
				task.run(tm);
			}

			final CyNetwork network = reader.getNetworks()[0];
			newSuid = network.getSUID();
			final CyRootNetwork rootNetwork = ((CySubNetwork) network).getRootNetwork();
			final CyTable table = rootNetwork.getDefaultNetworkTable();
			final CyColumn singletonColumn = table.getColumn(NdexStatusResource.SINGLETON_COLUMN_NAME);

			if (singletonColumn == null) {
				table.createColumn(NdexStatusResource.SINGLETON_COLUMN_NAME, Boolean.class, true);
			}

			table.getRow(rootNetwork.getSUID()).set(NdexStatusResource.SINGLETON_COLUMN_NAME, createdFromSingleton);

			// Create special column only for networks created from singleton
			if (createdFromSingleton) {
				final CyColumn originalUuidColumn = table.getColumn(ORIGINAL_UUID_TAG);

				if (originalUuidColumn == null) {
					table.createColumn(ORIGINAL_UUID_TAG, String.class, true);
				}
				table.getRow(rootNetwork.getSUID()).set(ORIGINAL_UUID_TAG, params.uuid);
			}
		} catch (Exception e) {
			logger.error("Failed to load network from NDEx", e);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, "Failed to load network from NDEx.",
					ErrorType.INTERNAL);
		}

		final NdexBaseResponse response = new NdexBaseResponse(newSuid, params.uuid);
		try {
			return CIProvider.getCIResponseFactory().getCIResponse(response, CINdexBaseResponse.class);
		} catch (InstantiationException | IllegalAccessException e) {
			final String message = "Could not create wrapped CI JSON.";
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}

	@Override
	@CIWrapping
	public CINdexBaseResponse saveNetworkToNdex(final Long suid, final NdexSaveParameters params) {

		// Return error if SUID of the network is not available
		if (suid == null) {
			logger.error("SUID is missing");
			throw errorBuilder.buildException(Status.BAD_REQUEST, "SUID is not specified.",
					ErrorType.INVALID_PARAMETERS);
		}

		final CyNetwork network = networkManager.getNetwork(suid);

		// Invalid SUID
		if (network == null) {
			final String message = "Network with SUID " + suid + " does not exist.";
			logger.error(message);
			throw errorBuilder.buildException(Status.NOT_FOUND, message, ErrorType.INVALID_PARAMETERS);
		}

		// Need to update the local table BEFORE saving it

		// Metadata provided from the web UI
		final Map<String, String> metadata = params.metadata;
		final CyRootNetwork root = ((CySubNetwork) network).getRootNetwork();
		// final CyTable rootTable = root.getDefaultNetworkTable();
		CyTable table = network.getDefaultNetworkTable();

		// Set Metadata to collection's table
		for (String key : metadata.keySet()) {
			saveMetadata(key, metadata.get(key), root);
		}

		// Get writer to convert collection into CX
		final CyNetworkViewWriterFactory writerFactory = tfManager.getCxWriterFactory();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		final CyWriter writer = writerFactory.createWriter(os, network);

		try {
			writer.run(new HeadlessTaskMonitor());
		} catch (Exception e) {
			final String message = "Failed to write network as CX";
			logger.error(message, e);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}

		// Upload to NDEx
		String networkName = network.getDefaultNetworkTable().getRow(network.getSUID()).get(CyNetwork.NAME,
				String.class);
		final ByteArrayInputStream cxis = new ByteArrayInputStream(os.toByteArray());

		String newUuid = null;
		newUuid = client.postNetwork(params.serverUrl + "/network", networkName, cxis, params.userId, params.password);

		if (newUuid == null || newUuid.isEmpty()) {
			final String message = "Failed to upload CX to NDEx.  (NDEx did not return UUID)";
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.NDEX_API);
		}

		// Assign new UUID to the network collection
		saveMetadata(NdexStatusResource.NDEX_UUID_TAG, newUuid, root);

		// Visibility
		if (params.isPublic) {
			int retries = 0;
			for (; retries < 5; retries++) {
				try {
					this.setVisibility(params, newUuid);
					break;
				} catch (Exception e) {
					String message = String.format("Error updating visibility. Retrying (%d/5)...", retries);
					logger.error(message);
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
			if (retries == 5){
				final String message = "NDEx appears to be busy.\n" + 
			"Your network will likely be saved in your account, but will remain private. \n" +
			"You can use the NDEx web site to make your network public once NDEx posts it there.";
				logger.error(message);
				throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
			}
		}

		final NdexBaseResponse response = new NdexBaseResponse(suid, newUuid);
		try {
			return CIProvider.getCIResponseFactory().getCIResponse(response, CINdexBaseResponse.class);
		} catch (InstantiationException | IllegalAccessException e) {
			final String message = "Could not create wrapped CI JSON.";
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}

	private final void setVisibility(final NdexSaveParameters params, final String uuid) {
		// This is a hack: NDEx does not respond immediately after creation.
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			final String message = "Failed to wait (This should not happen!)";
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}

		client.setVisibility(params.serverUrl, uuid, params.isPublic, params.userId, params.password);
	}

	private final void saveMetadata(String columnName, String value, CyRootNetwork root) {

		final CyTable rootLocalTable = root.getTable(CyNetwork.class, CyNetwork.LOCAL_ATTRS);
		final CyRow row = rootLocalTable.getRow(root.getSUID());

		// Create new column if it does not exist
		final CyColumn col = rootLocalTable.getColumn(columnName);
		if (col == null) {
			rootLocalTable.createColumn(columnName, String.class, false);
		}

		// Set the value to local table
		row.set(columnName, value);
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
			return CIProvider.getCIResponseFactory().getCIResponse(response, CISummaryResponse.class);
		} catch (InstantiationException | IllegalAccessException e) {
			final String message = "Could not create wrapped CI JSON.";
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}

	private final SummaryResponse buildSummary(final CyRootNetwork root, final CySubNetwork network) {
		final SummaryResponse summary = new SummaryResponse();

		// Network local table
		final NetworkSummary rootSummary = buildNetworkSummary(root, root.getDefaultNetworkTable(), root.getSUID());
		summary.currentNetworkSuid = network.getSUID();
		summary.currentRootNetwork = rootSummary;
		List<NetworkSummary> members = new ArrayList<>();
		root.getSubNetworkList().stream().forEach(
				subnet -> members.add(buildNetworkSummary(subnet, subnet.getDefaultNetworkTable(), subnet.getSUID())));
		summary.members = members;

		return summary;
	}

	private final NetworkSummary buildNetworkSummary(CyNetwork network, CyTable table, Long networkSuid) {

		NetworkSummary summary = new NetworkSummary();
		CyRow row = table.getRow(networkSuid);
		summary.suid = row.get(CyNetwork.SUID, Long.class);
		// Get NAME from local table because this is always local.
		summary.name = network.getTable(CyNetwork.class, CyNetwork.LOCAL_ATTRS).getRow(network.getSUID())
				.get(CyNetwork.NAME, String.class);
		summary.uuid = row.get(NdexStatusResource.NDEX_UUID_TAG, String.class);

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

		final CyNetwork network = networkManager.getNetwork(suid);
		if (network == null) {
			final String message = "Network with SUID " + suid + " does not exist.";
			logger.error(message);
			throw errorBuilder.buildException(Status.NOT_FOUND, message, ErrorType.INVALID_PARAMETERS);
		}

		// Check UUID
		final CyRootNetwork root = ((CySubNetwork) network).getRootNetwork();
		final String uuid = root.getDefaultNetworkTable().getRow(root.getSUID()).get(NdexStatusResource.NDEX_UUID_TAG,
				String.class);

		// Update Cytoscape table first
		final Map<String, String> metadata = params.metadata;
		final CyTable rootTable = root.getDefaultNetworkTable();

		for (String key : metadata.keySet()) {
			saveMetadata(key, metadata.get(key), root);
		}
		final CyNetworkViewWriterFactory writerFactory = tfManager.getCxWriterFactory();

		int retryCount = 0;
		boolean success = false;
		while (retryCount <= 3) {
			try {
				success = updateExistingNetwork(writerFactory, network, params, uuid);
				if (success) {
					break;
				}
			} catch (Exception e) {
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}

			} finally {
				retryCount++;
			}
		}

		if (!success) {
			final String message = "Could not update existing NDEx entry.  NDEx server did not accept your request.";
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}

		// Visibility
		if (params.isPublic != null) {
			this.setVisibility(params, uuid);
		}

		final NdexBaseResponse response = new NdexBaseResponse(suid, uuid);
		try {
			return CIProvider.getCIResponseFactory().getCIResponse(response, CINdexBaseResponse.class);
		} catch (InstantiationException | IllegalAccessException e) {
			final String message = "Could not create wrapped CI JSON.";
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}

	private final boolean updateExistingNetwork(final CyNetworkViewWriterFactory writerFactory, final CyNetwork network,
			final NdexSaveParameters params, final String uuid) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		final CyWriter writer = writerFactory.createWriter(os, network);

		try {
			writer.run(new HeadlessTaskMonitor());
		} catch (Exception e) {
			final String message = "Failed to write network as CX";
			logger.error(message, e);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}

		// Upload to NDEx
		final ByteArrayInputStream cxis = new ByteArrayInputStream(os.toByteArray());

		try {
			// Ndex client from NDEx Team
			final NdexRestClient nc = new NdexRestClient(params.userId, params.password, params.serverUrl);
			final NdexRestClientModelAccessLayer ndex = new NdexRestClientModelAccessLayer(nc);
			ndex.updateCXNetwork(UUID.fromString(uuid), cxis);

		} catch (Exception e1) {
			final String message = "Failed to update network. Please check UUID and make sure it exists in NDEx.";
			logger.error(message, e1);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.NDEX_API);
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

}
