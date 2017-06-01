package org.cytoscape.hybrid.internal.task;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.hybrid.events.ExternalAppStartedEvent;
import org.cytoscape.hybrid.internal.ws.ExternalAppManager;
import org.cytoscape.hybrid.internal.ws.WSClient;
import org.cytoscape.property.CyProperty;
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
	private final CyProperty<Properties> props;
	private final CyEventHelper eventHelper;

	final String WS_LOCATION = "ws://localhost:8025/ws/echo";

	public OpenExternalAppTask(final String appName, final WSClient client, final ExternalAppManager pm,
			final String command, final CyProperty<Properties> props, final CyEventHelper eventHelper) {
		this.client = client;
		this.command = command;
		this.pm = pm;
		this.appName = appName;
		this.props = props;
		this.eventHelper = eventHelper;
	}
	
	public void configure(Object config) {
		
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {

		final String cyrestPort = props.getProperties().get("rest.port").toString();
		
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
				pm.setProcess(Runtime.getRuntime().exec(command + " " + cyrestPort));
					
				eventHelper.fireEvent(new ExternalAppStartedEvent(this));
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("Could not start the application: " + appName, e);
			}
		});
	}
}
