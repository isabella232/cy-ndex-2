package org.cytoscape.hybrid.internal.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.cytoscape.hybrid.internal.CxTaskFactoryManager;
import org.cytoscape.hybrid.internal.rest.reader.CxReaderFactory;
import org.cytoscape.hybrid.internal.rest.reader.UpdateTableTask;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

public class NdexImportResourceImpl implements NdexImportResource {

	private final NdexClient client;
	private final TaskMonitor tm;

	private CxTaskFactoryManager tfManager;
	private final CxReaderFactory loadNetworkTF;

	public NdexImportResourceImpl(CxTaskFactoryManager tfManager, TaskFactory loadNetworkTF) {

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
			summary = client.getSummary(params.getNdexServerUrl(), params.getUuid(), params.getUserId(), params.getToken());
			System.out.println(summary);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("############################\n\n SUMMARY = " + summary);
		
		// Load network from ndex
		InputStream is;
		try {
			is = client.load(params.getNdexServerUrl() + "/network/" + params.getUuid(), params.getUserId(), params.getToken());
			InputStreamTaskFactory readerTF = this.tfManager.getCxReaderFactory();
			TaskIterator itr = readerTF.createTaskIterator(is, "ndexCollection");
			CyNetworkReader reader = (CyNetworkReader) itr.next();
			TaskIterator tasks = loadNetworkTF.createTaskIterator(summary.get("name").toString(), reader);

			// Update table AFTER loading
			Task updateTableTask = new UpdateTableTask(reader);
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
}
