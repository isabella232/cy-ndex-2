package org.cytoscape.hybrid.internal.rest.endpoints.impl;

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
import org.cytoscape.hybrid.internal.CxTaskFactoryManager;
import org.cytoscape.hybrid.internal.rest.HeadlessTaskMonitor;
import org.cytoscape.hybrid.internal.rest.NdexClient;
import org.cytoscape.hybrid.internal.rest.NetworkSummary;
import org.cytoscape.hybrid.internal.rest.endpoints.NdexNetworkResource;
import org.cytoscape.hybrid.internal.rest.errors.ErrorBuilder;
import org.cytoscape.hybrid.internal.rest.errors.ErrorType;
import org.cytoscape.hybrid.internal.rest.parameter.NdexImportParams;
import org.cytoscape.hybrid.internal.rest.parameter.NdexSaveParameters;
import org.cytoscape.hybrid.internal.rest.reader.CxReaderFactory;
import org.cytoscape.hybrid.internal.rest.reader.UpdateTableTask;
import org.cytoscape.hybrid.internal.rest.response.NdexBaseResponse;
import org.cytoscape.hybrid.internal.rest.response.SummaryResponse;
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

	private final NdexClient client;
	private final TaskMonitor tm;

	private CxTaskFactoryManager tfManager;

	private final CxReaderFactory loadNetworkTF;

	private final CyNetworkManager networkManager;
	private final CyApplicationManager appManager;

	private final CIExceptionFactory ciExceptionFactory;
	private final CIErrorFactory ciErrorFactory;

	private final ErrorBuilder errorBuilder;
	
	public NdexNetworkResourceImpl(final NdexClient client, final ErrorBuilder errorBuilder, CyApplicationManager appManager, CyNetworkManager networkManager,
			CxTaskFactoryManager tfManager, TaskFactory loadNetworkTF,
			CIExceptionFactory ciExceptionFactory, CIErrorFactory ciErrorFactory) { 

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
	public NdexBaseResponse createNetworkFromNdex(final NdexImportParams params) {

		// 1. Get summary of the network.
		Map<String, ?> summary = null;

		summary = client.getSummary(params.serverUrl, params.uuid, params.userId, params.password);

		// Load network from ndex
		InputStream is;
		Long newSuid = null;

		is = client.load(params.serverUrl + "/network/" + params.uuid, params.userId,
				params.password);

		try {
			InputStreamTaskFactory readerTF = this.tfManager.getCxReaderFactory();
			TaskIterator itr = readerTF.createTaskIterator(is, "ndexCollection");
			CyNetworkReader reader = (CyNetworkReader) itr.next();
			TaskIterator tasks = loadNetworkTF.createTaskIterator(summary.get("name").toString(), reader);

			// Update table AFTER loading
			UpdateTableTask updateTableTask = new UpdateTableTask(reader);
			updateTableTask.setUuid(params.uuid);
			tasks.append(updateTableTask);

			while (tasks.hasNext()) {
				final Task task = tasks.next();
				task.run(tm);
			}

			newSuid = reader.getNetworks()[0].getSUID();
		} catch (Exception e) {
			logger.error("Failed to load network from NDEx", e);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR,
					"Failed to load network from NDEx.", ErrorType.INTERNAL);
		}

		return new NdexBaseResponse(newSuid, params.uuid);
	}

	
	@Override
	@CIWrapping
	public NdexBaseResponse saveNetworkToNdex(final Long suid, final NdexSaveParameters params) {

		// Return error if SUID of the network is not available
		if (suid == null) {
			logger.error("SUID is missing");
			throw errorBuilder.buildException(Status.BAD_REQUEST, "SUID is not specified.", ErrorType.INVALID_PARAMETERS);
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
		final CyTable rootTable = root.getDefaultNetworkTable();
		
		// Set Metadata to collection's table
		metadata.keySet().stream()
			.forEach(key -> saveMetadata(key, metadata.get(key), rootTable, root.getSUID()));
		

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
		newUuid = client.postNetwork(params.serverUrl + "/network", networkName, cxis, params.userId,
				params.password);
		
		if (newUuid == null || newUuid.isEmpty()) {
			final String message = "Failed to upload CX to NDEx.  (NDEx did not return UUID)";
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.NDEX_API);
		}

		// Assign new UUID to the network collection
		saveMetadata(NdexClient.UUID_COLUMN_NAME, newUuid, rootTable, root.getSUID());

		// Visibility
		if (params.isPublic) {
			// This is a hack: NDEx does not respond immediately after creation.
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				final String message = "Failed to wait (This should not happen!)";
				logger.error(message);
				throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
			}

			client.setVisibility(params.serverUrl, newUuid, true, params.userId, params.password);
		}

		return new NdexBaseResponse(suid, newUuid);
	}

	private final void saveMetadata(String columnName, String value, CyTable table, Long suid) {
		final CyColumn col = table.getColumn(columnName);

		if (col == null) {
			table.createColumn(columnName, String.class, false);
		}
		table.getRow(suid).set(columnName, value);
	}

	@Override
	@CIWrapping
	public NdexBaseResponse saveCurrentNetworkToNdex(NdexSaveParameters params) {
		final CyNetwork network = appManager.getCurrentNetwork();
		if (network == null) {
			// Current network is not available
			final String message = "Current network does not exist.  You need to choose a network first.";
			logger.error(message);
			final CIError ciError = 
					ciErrorFactory.getCIError(
							Status.BAD_REQUEST.getStatusCode(), 
							"urn:cytoscape:ci:ndex:v1:errors:1", 
							message,
							URI.create("file:///log"));
		    throw ciExceptionFactory.getCIException(
		    		Status.BAD_REQUEST.getStatusCode(), 
		    		new CIError[]{ciError});
		}

		return saveNetworkToNdex(network.getSUID(), params);
	}

	@CIWrapping
	@Override
	public SummaryResponse getCurrentNetworkSummary() {
		final CyNetwork network = appManager.getCurrentNetwork();
		
		if (network == null) {
			final String message = "Current network does not exist (No network is selected)";
			logger.error(message);
			throw errorBuilder.buildException(Status.BAD_REQUEST, message, ErrorType.INTERNAL);
		}
		
		final CyRootNetwork root = ((CySubNetwork) network).getRootNetwork();

		return buildSummary(root, (CySubNetwork) network);
	}

	private final SummaryResponse buildSummary(final CyRootNetwork root, final CySubNetwork network) {
		final SummaryResponse summary = new SummaryResponse();

		// Network local table
		final NetworkSummary rootSummary = buildNetworkSummary(root);
		summary.currentNetworkSuid = network.getSUID();
		summary.currentRootNetwork = rootSummary;
		List<NetworkSummary> members = new ArrayList<>();
		root.getSubNetworkList().stream().forEach(subnet -> members.add(buildNetworkSummary(subnet)));
		summary.members = members;

		return summary;
	}

	private final NetworkSummary buildNetworkSummary(final CyNetwork network) {
		CyTable table = network.getDefaultNetworkTable();
		NetworkSummary summary = new NetworkSummary();
		CyRow row = table.getRow(network.getSUID());
		summary.suid = row.get(CyNetwork.SUID, Long.class);
		summary.name = network.getTable(CyNetwork.class, CyNetwork.LOCAL_ATTRS).getRow(network.getSUID())
				.get(CyNetwork.NAME, String.class);
		summary.uuid = row.get("uuid", String.class);

		final Collection<CyColumn> columns = table.getColumns();
		final Map<String, Object> props = new HashMap<>();

		columns.stream().forEach(col -> props.put(col.getName(), row.get(col.getName(), col.getType())));
		summary.props = props;

		return summary;
	}

	@Override
	@CIWrapping
	public NdexBaseResponse updateNetworkInNdex(Long suid, NdexSaveParameters params) {
		
		if (suid == null) {
			logger.error("SUID is missing");
			throw errorBuilder.buildException(Status.BAD_REQUEST, "SUID is not specified.", ErrorType.INVALID_PARAMETERS);
		}

		final CyNetwork network = networkManager.getNetwork(suid);
		if (network == null) {
			final String message = "Network with SUID " + suid + " does not exist.";
			logger.error(message);
			throw errorBuilder.buildException(Status.NOT_FOUND, message, ErrorType.INVALID_PARAMETERS);
		}
		
		// Check UUID
		final CyRootNetwork root = ((CySubNetwork)network).getRootNetwork();
		final String uuid = root.getDefaultNetworkTable().getRow(root.getSUID()).get("ndex.uuid", String.class);
		
		// Update Cytoscape table first
		final Map<String, String> metadata = params.metadata;
		final CyTable rootTable = root.getDefaultNetworkTable();
		metadata.keySet().stream().forEach(key -> saveMetadata(key, metadata.get(key), rootTable, root.getSUID()));

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
		final ByteArrayInputStream cxis = new ByteArrayInputStream(os.toByteArray());

		try {
			// Ndex client from NDEx Team
			final NdexRestClient nc = new NdexRestClient(params.userId, params.password,
					params.serverUrl);
			final NdexRestClientModelAccessLayer ndex = new NdexRestClientModelAccessLayer(nc);
			ndex.updateCXNetwork(UUID.fromString(uuid), cxis);

		} catch (Exception e1) {
			e1.printStackTrace();
			final String message = "Failed to update network. Please check UUID and make sure it exists in NDEx.";
			logger.error(message, e1);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.NDEX_API);
		}

		// Visibility
		if (params.isPublic) {
			// This is a hack: NDEx does not respond immediately after creation.
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				final String message = "Failed to wait (This should not happen!)";
				logger.error(message);
				throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
			}
			client.setVisibility(params.serverUrl, uuid, true, params.userId, params.password);
		}

		return new NdexBaseResponse(suid, uuid);
	}

	@Override
	@CIWrapping
	public NdexBaseResponse updateCurrentNetworkInNdex(NdexSaveParameters params) {
		final CyNetwork network = appManager.getCurrentNetwork();
		
		if (network == null) {
			final String message = "Current network does not exist (No network is selected)";
			logger.error(message);
			throw errorBuilder.buildException(Status.BAD_REQUEST, message, ErrorType.INTERNAL);
		}
		return updateNetworkInNdex(network.getSUID(), params);
	}
	
}
