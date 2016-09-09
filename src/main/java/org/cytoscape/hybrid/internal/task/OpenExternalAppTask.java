package org.cytoscape.hybrid.internal.task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.cytoscape.hybrid.internal.ws.ExternalAppManager;
import org.cytoscape.hybrid.internal.ws.WSClient;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

public class OpenExternalAppTask extends AbstractTask {

	private final String appName;
	private final WSClient client;
	private final ExternalAppManager pm;
	private final String command;

	final String WS_LOCATION = "ws://localhost:8025/ws/echo";
		
	public OpenExternalAppTask(final String appName, final WSClient client, final ExternalAppManager pm,
			final String command) {
		this.client = client;
		this.command = command;
		this.pm = pm;
		this.appName = appName;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
	
		if(client.isStopped()) {
			client.start(WS_LOCATION);
		}
		
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			try {
				// Set application type:
				this.client.getSocket().setApplication(appName);
				pm.setProcess(Runtime.getRuntime().exec(command));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}
