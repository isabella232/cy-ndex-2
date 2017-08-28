package org.cytoscape.cyndex2.internal.task;

import java.awt.BorderLayout;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFrame;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.cyndex2.events.ExternalAppStartedEvent;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.property.CyProperty;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import com.teamdev.jxbrowser.chromium.swing.BrowserView;

/**
 * 
 * Task to execute a command and open a new external application as a new
 * process.
 *
 */
public class OpenExternalAppTask extends AbstractTask {

	// Name of the application
	private final String appName;

	private final CyProperty<Properties> props;
	private final CyEventHelper eventHelper;
	private final BrowserView browserView;
	private final ExternalAppManager pm;

	final String WS_LOCATION = "ws://localhost:8025/ws/echo";

	public OpenExternalAppTask(final String appName, final ExternalAppManager pm,
			final CyProperty<Properties> props, final CyEventHelper eventHelper, final BrowserView browserView) {
		this.pm = pm;
		this.appName = appName;
		this.props = props;
		this.eventHelper = eventHelper;
		this.browserView = browserView;
	}
	
	public void configure(Object config) {
		
		
	}
	

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {

		final String cyrestPort = props.getProperties().get("rest.port").toString();
		
		// Make sure WS server is running.

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
				//pm.setProcess(Runtime.getRuntime().exec(command + " " + cyrestPort));
					
				eventHelper.fireEvent(new ExternalAppStartedEvent(this));
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("Could not start the application: " + appName, e);
			}
		});
		JFrame frame = new JFrame();
        frame.add(browserView, BorderLayout.CENTER);
        frame.setSize(700, 500);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
		browserView.getBrowser().loadURL("localhost:2222");
	}
}
