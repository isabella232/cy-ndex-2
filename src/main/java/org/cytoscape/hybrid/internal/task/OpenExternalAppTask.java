package org.cytoscape.hybrid.internal.task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.cytoscape.hybrid.internal.ws.ExternalAppManager;
import org.cytoscape.hybrid.internal.ws.WSClient;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

/**
 * 
 * Task to execute a command and open a new external application as a new
 * process.
 *
 */
public class OpenExternalAppTask extends AbstractTask {

	// Name of the application
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
	
	public void configure(Object config) {
		
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {

		// Make sure WS server is running.
		if (client.isStopped()) {
			client.start(WS_LOCATION);
		}

		pm.setAppName(appName);
		
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			try {
				// Close other application
				try {
					pm.kill();
					Thread.sleep(400);
				} catch (Exception e2) {
					e2.printStackTrace();
					throw new RuntimeException("Could not stop existing app instance.");
				}

				// Set application type:
				this.client.getSocket().setApplication(appName);
				pm.setProcess(Runtime.getRuntime().exec(command));
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("Could not start the application: " + appName, e);
			}
		});
	}
}
