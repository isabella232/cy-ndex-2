package org.cytoscape.cyndex2.internal.rest.endpoints.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import org.cytoscape.ci.CIWrapping;
import org.cytoscape.ci.model.CIError;
import org.cytoscape.ci_bridge_impl.CIProvider;
import org.cytoscape.cyndex2.internal.CxTaskFactoryManager;
import org.cytoscape.cyndex2.internal.rest.HeadlessTaskMonitor;
import org.cytoscape.cyndex2.internal.rest.NdexClient;
import org.cytoscape.cyndex2.internal.rest.SimpleNetworkSummary;
import org.cytoscape.cyndex2.internal.rest.endpoints.NdexNetworkResource;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorBuilder;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorType;
import org.cytoscape.cyndex2.internal.rest.parameter.NdexImportParams;
import org.cytoscape.cyndex2.internal.rest.parameter.NdexSaveParameters;
import org.cytoscape.cyndex2.internal.rest.parameter.NdexUpdateParameters;
import org.cytoscape.cyndex2.internal.rest.response.NdexBaseResponse;
import org.cytoscape.cyndex2.internal.rest.response.SummaryResponse;
import org.cytoscape.cyndex2.internal.singletons.CXInfoHolder;
import org.cytoscape.cyndex2.internal.singletons.CyObjectManager;
import org.cytoscape.cyndex2.internal.singletons.NetworkManager;
import org.cytoscape.cyndex2.internal.task.NetworkExportTask;
import org.cytoscape.cyndex2.internal.task.NetworkExportTask.NetworkExportException;
import org.cytoscape.cyndex2.internal.task.NetworkImportTask;
import org.cytoscape.io.write.CyNetworkViewWriterFactory;
import org.cytoscape.io.write.CyWriter;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.work.TaskFactory;
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

	private CxTaskFactoryManager tfManager;

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
	}

	@Override
	@CIWrapping
	public CINdexBaseResponse createNetworkFromNdex(final NdexImportParams params) {

		NetworkImportTask importer;
		try {
			importer = new NetworkImportTask(params.userId, params.password, params.serverUrl, params.uuid);
			importer.run(tm);
		} catch (IOException | NdexException e2) {
			final String message = "Failed to connect to server and retrieve network.";
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);

		} catch (Exception e) {
			final String message = "Unable to create CyNetwork from NDEx.\n" + e.getMessage();
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);

		}

		final NdexBaseResponse response = new NdexBaseResponse(importer.getSUID(), params.uuid);
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

		CyNetwork network = CyObjectManager.INSTANCE.getNetworkManager().getNetwork(suid);
		if (network == null) {
			// Current network is not available
			final String message = "Network with SUID " + String.valueOf(suid) + " does not exist.";
			logger.error(message);
			final CIError ciError = ciErrorFactory.getCIError(Status.BAD_REQUEST.getStatusCode(),
					"urn:cytoscape:ci:ndex:v1:errors:1", message, URI.create("file:///log"));
			throw ciExceptionFactory.getCIException(Status.BAD_REQUEST.getStatusCode(), new CIError[] { ciError });
		}

		// Save non-null metadata to network/collection table
		CyRootNetwork root = ((CySubNetwork) network).getRootNetwork();
		for (String column : params.metadata.keySet()) {
			saveMetadata(column, params.metadata.get(column), params.writeCollection ? root : network);
		}
		NetworkExportTask exporter = new NetworkExportTask(network, params);
		try {
			exporter.run(tm);
			String newUUID = exporter.getNetworkUUID().toString();

			if (params.isPublic) {
				setVisibility(params, newUUID);
			}

			final NdexBaseResponse response = new NdexBaseResponse(suid, newUUID);
			return CIProvider.getCIResponseFactory().getCIResponse(response, CINdexBaseResponse.class);
		} catch (NetworkExportException e1) {
			final String message = "Error exporting network to CX:" + e1.getMessage();
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		} catch (InstantiationException | IllegalAccessException e2) {
			final String message = "Could not create wrapped CI JSON response.";
			logger.error(message);
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
				client.setVisibility(params.serverUrl, uuid, params.isPublic, params.userId, params.password);
				break;
			} catch (Exception e) {
				String message = String.format("Error updating visibility. Retrying (%d/5)...", retries);
				logger.warn(message);
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					message = "Failed to wait. This should never happen.";
					logger.error(message);
					final CIError ciError = ciErrorFactory.getCIError(Status.BAD_REQUEST.getStatusCode(),
							"urn:cytoscape:ci:ndex:v1:errors:1", message);
					throw ciExceptionFactory.getCIException(Status.BAD_REQUEST.getStatusCode(),
							new CIError[] { ciError });

				}
			}
			if (retries == 5) {
				final String message = "NDEx appears to be busy.\n"
						+ "Your network will likely be saved in your account, but will remain private. \n"
						+ "You can use the NDEx web site to make your network public once NDEx posts it there.";
				logger.warn(message);
				throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
			}
		}

	}

	private final void saveMetadata(String columnName, String value, CyNetwork network) {

		final CyTable localTable = network.getTable(CyNetwork.class, CyNetwork.LOCAL_ATTRS);
		final CyRow row = localTable.getRow(network.getSUID());

		// Create new column if it does not exist
		final CyColumn col = localTable.getColumn(columnName);
		if (col == null) {
			if (value != null && !value.isEmpty())
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
		final SimpleNetworkSummary rootSummary = buildNetworkSummary(root, root.getDefaultNetworkTable(),
				root.getSUID());
		summary.currentNetworkSuid = network.getSUID();
		summary.currentRootNetwork = rootSummary;
		List<SimpleNetworkSummary> members = new ArrayList<>();
		root.getSubNetworkList().stream().forEach(
				subnet -> members.add(buildNetworkSummary(subnet, subnet.getDefaultNetworkTable(), subnet.getSUID())));
		summary.members = members;

		return summary;
	}

	private final SimpleNetworkSummary buildNetworkSummary(CyNetwork network, CyTable table, Long networkSuid) {

		SimpleNetworkSummary summary = new SimpleNetworkSummary();
		CyRow row = table.getRow(networkSuid);
		summary.suid = row.get(CyNetwork.SUID, Long.class);
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
	public CINdexBaseResponse updateNetworkInNdex(Long suid, NdexUpdateParameters params) {

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
		if (updateIsPossibleHelper(suid, params) == null){
			final String message = "Unable to update network in NDEx, do you own this network?";
			logger.error(message);
			throw errorBuilder.buildException(Status.BAD_REQUEST, message,
					ErrorType.INVALID_PARAMETERS);
		}
		
		final UUID uuid = NetworkManager.INSTANCE.getNdexNetworkId(suid);
		String uuidStr = uuid == null ? null : uuid.toString();

		for (String key : params.metadata.keySet()) {
			saveMetadata(key, params.metadata.get(key), params.writeCollection == true ? root : network);
		}
		final CyNetworkViewWriterFactory writerFactory = tfManager.getCxWriterFactory();

		int retryCount = 0;
		boolean success = false;
		while (retryCount <= 3) {
			try {
				success = updateExistingNetwork(writerFactory, network, params, uuid.toString());
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
		if (params.isPublic == true) {
			this.setVisibility(params, uuidStr);
		}

		final NdexBaseResponse response = new NdexBaseResponse(suid, uuidStr);
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
	public CINdexBaseResponse updateCurrentNetworkInNdex(NdexUpdateParameters params) {
		final CyNetwork network = appManager.getCurrentNetwork();
		if (network == null) {
			final String message = "Current network does not exist (No network is selected)";
			logger.error(message);
			throw errorBuilder.buildException(Status.BAD_REQUEST, message, ErrorType.INTERNAL);
		}
		return updateNetworkInNdex(network.getSUID(), params);
	}

	private NetworkSummary updateIsPossibleHelper(final long suid, final NdexSaveParameters params) {

		CyNetwork cyNetwork = CyObjectManager.INSTANCE.getNetworkManager().getNetwork(suid);
		UUID ndexNetworkId = null;
		CXInfoHolder cxInfo = NetworkManager.INSTANCE.getCXInfoHolder(cyNetwork.getSUID());
		if (cxInfo != null) {
			ndexNetworkId = cxInfo.getNetworkId();
		} else {
			ndexNetworkId = NetworkManager.INSTANCE.getNdexNetworkId(cyNetwork.getSUID());
		}

		if (ndexNetworkId == null)
			return null;

		final NdexRestClient nc = new NdexRestClient(params.userId, params.password, params.serverUrl);
		final NdexRestClientModelAccessLayer mal = new NdexRestClientModelAccessLayer(nc);
		try {
			Map<String, Permissions> permissionTable = mal.getUserNetworkPermission(nc.getUserUid(), ndexNetworkId,
					false);
			if (permissionTable == null || permissionTable.get(ndexNetworkId.toString()) == Permissions.READ)
				return null;

		} catch (IOException e) {
			return null;
		}

		NetworkSummary ns = null;
		try {
			ns = mal.getNetworkSummaryById(ndexNetworkId.toString());
			if (ns.getIsReadOnly())
				return null;

		} catch (IOException e) {
			return null;
		} catch (NdexException e) {
			return null;
		}
		return ns;
	}
}
