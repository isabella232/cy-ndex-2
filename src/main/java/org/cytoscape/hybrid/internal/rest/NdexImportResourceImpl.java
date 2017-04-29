package org.cytoscape.hybrid.internal.rest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.InternalServerErrorException;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.hybrid.internal.CxTaskFactoryManager;
import org.cytoscape.hybrid.internal.rest.reader.CxReaderFactory;
import org.cytoscape.hybrid.internal.rest.reader.UpdateTableTask;
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

public class NdexImportResourceImpl implements NdexImportResource {

	private final NdexClient client;
	private final TaskMonitor tm;

	private CxTaskFactoryManager tfManager;
	
	private final CxReaderFactory loadNetworkTF;
	
	private final CyNetworkManager networkManager;
	private final CyApplicationManager appManager;

	public NdexImportResourceImpl(CyApplicationManager appManager, CyNetworkManager networkManager, CxTaskFactoryManager tfManager, TaskFactory loadNetworkTF) {

		this.networkManager = networkManager;
		this.appManager = appManager;
		
		this.client = new NdexClient();
		this.tm = new HeadlessTaskMonitor();
		this.tfManager = tfManager;
		this.loadNetworkTF = (CxReaderFactory) loadNetworkTF;
	}


	@Override
	public NdexResponse<NdexImportResponse> createNetworkFromNdex(final NdexImportParams params) {

		// Prepare base response
		final NdexResponse<NdexImportResponse> response = new NdexResponse<>();
		
		// 1. Get summary of the network.
		Map<String, ?> summary = null;
		
		try {
			summary = client.getSummary(params.getServerUrl(), params.getUuid(), params.getUserId(), params.getPassword());
			System.out.println(summary);
		} catch (IOException e) {
			final CIError error = new CIError(500, "", e.getMessage(), "");
			response.getErrors().add(error);
			return response;
		}
		
		System.out.println("############################\n\n SUMMARY = " + summary);
		
		// Load network from ndex
		InputStream is;
		Long newSuid = null;
		
		try {
			is = client.load(params.getServerUrl() + "/network/" + params.getUuid(), params.getUserId(), params.getPassword());
			InputStreamTaskFactory readerTF = this.tfManager.getCxReaderFactory();
			TaskIterator itr = readerTF.createTaskIterator(is, "ndexCollection");
			CyNetworkReader reader = (CyNetworkReader) itr.next();
			TaskIterator tasks = loadNetworkTF.createTaskIterator(summary.get("name").toString(), reader);

			// Update table AFTER loading
			UpdateTableTask updateTableTask = new UpdateTableTask(reader);
			updateTableTask.setUuid(params.getUuid());
			tasks.append(updateTableTask);			
			
			while (tasks.hasNext()) {
				final Task task = tasks.next();
				System.out.println("----------------- RUN: " + task);
				task.run(tm);
			}
			
			newSuid = reader.getNetworks()[0].getSUID();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Could not load NDEx network: " + params.getUuid(), e);
		}
		
		System.out.println("############################ DONE! #############################");

		response.setData(new NdexImportResponse(newSuid, params.getUuid()));
		return response;
	}

	@Override
	public NdexResponse<NdexSaveResponse> saveNetworkToNdex(Long suid, NdexSaveParams params) {
		
		System.out.println("######## SAVE SUID = " + suid);
		System.out.println("######## SAVE params = " + params);
		
		final NdexResponse<NdexSaveResponse> response = new NdexResponse<>();
		
		if(suid == null) {
			throw new NullPointerException("SUID is required.");
		}
		
		final CyNetwork network = networkManager.getNetwork(suid);
		if(network == null) {
			throw new IllegalStateException("No such network: " + suid);
		}
		
		
		final CyNetworkViewWriterFactory writerFactory = tfManager.getCxWriterFactory();
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		final CyWriter writer = writerFactory.createWriter(os, network);
		
		try {
			writer.run(new HeadlessTaskMonitor());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Upload to NDEx
		String networkName = network.getDefaultNetworkTable().getRow(network.getSUID()).get(CyNetwork.NAME,String.class);
		NdexClient client = new NdexClient();
		final ByteArrayInputStream cxis = new ByteArrayInputStream(os.toByteArray());
		String newUuid = null;
		
		try {
			newUuid = client.postNetwork(params.getServerUrl() + "/network", networkName, cxis, params.getUserId(), params.getPassword());
		} catch (Exception e) {
			e.printStackTrace();
			throw new InternalServerErrorException("Could not save", e);
		}
		
		if(newUuid == null || newUuid.isEmpty()) {
			throw new InternalServerErrorException("Could not save network.");
		}
		
		// Update table
		final Map<String, String> metadata = params.getMetadata();
		final CyRootNetwork root = ((CySubNetwork)network).getRootNetwork();
		final CyTable rootTable = root.getDefaultNetworkTable();
		
		metadata.keySet().stream().forEach(key->saveMetadata(key, metadata.get(key), rootTable, root.getSUID()));
		
		// Visibility
		if(params.getIsPublic()) {
			// This is a hack: NDEx does not respond immediately after creation.
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			client.setVisibility(params.getServerUrl(), newUuid, true, params.getUserId(), params.getPassword());
		}
		
		response.setData(new NdexSaveResponse(suid, newUuid));
		return response;
	}
	
	
	private final void saveMetadata(String columnName, String value, CyTable table, Long suid) {
		final CyColumn col = table.getColumn(columnName);
		
		if(col == null) {
			table.createColumn(columnName, String.class, false);
		}
		table.getRow(suid).set(columnName, value);
	}


	@Override
	public NdexResponse<NdexSaveResponse> saveCurrentNetworkToNdex(NdexSaveParams params) {
		final CyNetwork network = appManager.getCurrentNetwork();
		if(network == null) {
			throw new IllegalStateException("Current network is null.");
		}
		
		return saveNetworkToNdex(network.getSUID(), params);
	}


	@Override
	public SummaryResponse getCurrentNetworkSummary() {
		final CyNetwork network = appManager.getCurrentNetwork();
		final CyRootNetwork root = ((CySubNetwork)network).getRootNetwork();
		
		return buildSummary(root, (CySubNetwork)network);
	}
	
	private final SummaryResponse buildSummary(final CyRootNetwork root, final CySubNetwork network) {
		final SummaryResponse summary = new SummaryResponse();
		
		// Network local table
		final NetworkSummary rootSummary = buildNetworkSummary(root);
		summary.currentNetworkSuid = network.getSUID();
		summary.currentRootNetwork = rootSummary;
		List<NetworkSummary> members = new ArrayList<>();
		root.getSubNetworkList().stream().forEach(subnet->members.add(buildNetworkSummary(subnet)));
		summary.members = members;
		
		return summary;
	}
	private final NetworkSummary buildNetworkSummary(final CyNetwork network) {
		CyTable table = network.getDefaultNetworkTable();
		NetworkSummary summary = new NetworkSummary();
		CyRow row = table.getRow(network.getSUID());
		summary.setSuid(row.get(CyNetwork.SUID, Long.class));
		summary.setName(network
				.getTable(CyNetwork.class, CyNetwork.LOCAL_ATTRS)
				.getRow(network.getSUID())
				.get(CyNetwork.NAME, String.class));
		summary.setNdexUuid(row.get("ndex.uuid", String.class));
		
		final Collection<CyColumn> columns = table.getColumns();
		final Map<String, Object> props = new HashMap<>();
		
		columns.stream().forEach(col->
			props.put(col.getName(), row.get(col.getName(),col.getType())));
		summary.setProps(props);
		
		return summary;
	}


	@Override
	public NdexResponse<NdexSaveResponse> updateNetworkInNdex(Long suid, NdexSaveParams params) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public NdexResponse<NdexSaveResponse> updateCurrentNetworkInNdex(NdexSaveParams params) {
		// TODO Auto-generated method stub
		return null;
	}
}
