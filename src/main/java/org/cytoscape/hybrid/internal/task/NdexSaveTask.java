package org.cytoscape.hybrid.internal.task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.cytoscape.hybrid.internal.ws.ExternalAppManager;
import org.cytoscape.hybrid.internal.ws.WSClient;
import org.cytoscape.task.AbstractNetworkViewTask;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskMonitor;


public class NdexSaveTask  extends AbstractNetworkViewTask {

	private static final String APPLICATION_SAVE = "ndex-save";
	
	private final WSClient client;
	private final ExternalAppManager pm;
	private final String command;


	public NdexSaveTask(CyNetworkView view, final WSClient client, final ExternalAppManager pm, String command) {
		super(view);
		this.client = client;
		this.command = command;
		this.pm = pm;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
	
		System.out.println("Ndex Save");
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			try {
				// Set application type:
				this.client.getSocket().setApplication(APPLICATION_SAVE);
				pm.setProcess(Runtime.getRuntime().exec(command));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}
