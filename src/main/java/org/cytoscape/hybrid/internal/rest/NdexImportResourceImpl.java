package org.cytoscape.hybrid.internal.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.hybrid.internal.CxTaskFactoryManager;
import org.cytoscape.hybrid.internal.rest.reader.CxReaderFactory;
import org.cytoscape.hybrid.internal.rest.reader.UpdateTableTask;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.write.CyNetworkViewWriterFactory;
import org.cytoscape.io.write.CyWriter;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
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

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{uuid}")
	public NdexImportResponse importNetwork(@PathParam("uuid") String uuid) {
		return new NdexImportResponse();
	}

	@Override
	public NdexImportResponse createNetworkFromNdex(NdexImportParams params) {

		System.out.println("############################\n\n **UUID = " + params.getUuid());

		Map<String, ?> summary = null;
		
		try {
			summary = client.getSummary(params.getNdexServerUrl(), params.getUuid(), params.getUserId(), params.getPassword());
			System.out.println(summary);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("############################\n\n SUMMARY = " + summary);
		
		// Load network from ndex
		InputStream is;
		try {
			is = client.load(params.getNdexServerUrl() + "/network/" + params.getUuid(), params.getUserId(), params.getPassword());
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
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("############################ DONE! #############################");

		return new NdexImportResponse();
	}

	@Override
	public NdexSaveResponse saveNetworkToNdex(Long suid, NdexImportParams params) {
		// TODO Auto-generated method stub
		
		System.out.println("######## SAVE SUID = " + suid);
		System.out.println("######## SAVE params = " + params);
		
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
		
		
		return new NdexSaveResponse(suid, "");
	}

	@Override
	public NdexSaveResponse saveCurrentNetworkToNdex(NdexImportParams params) {
		final CyNetwork network = appManager.getCurrentNetwork();
		if(network == null) {
			throw new IllegalStateException("Current network is null.");
		}
		
		return saveNetworkToNdex(network.getSUID(), params);
	}
}
