package org.cytoscape.hybrid.internal.task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.naming.spi.DirStateFactory.Result;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.hybrid.internal.ws.ExternalAppManager;
import org.cytoscape.hybrid.internal.ws.WSClient;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

public class OpenExternalAppTask extends AbstractTask {

	private final String appName;
	private final WSClient client;
	private final ExternalAppManager pm;
	private final String command;
	private final CyApplicationManager appManager;

	final String WS_LOCATION = "ws://localhost:8025/ws/echo";
		
	public OpenExternalAppTask(final String appName, final WSClient client, final ExternalAppManager pm,
			final String command, final CyApplicationManager appManager) {
		this.client = client;
		this.command = command;
		this.pm = pm;
		this.appName = appName;
		this.appManager = appManager;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		
		// Check pre-condition
		final String error = checkRequirments();
		if(error != null ) {
			// Show error message before running the job
		
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					JOptionPane.showMessageDialog(
							null, 
							error,
							"Error opening " + appName, 
							JOptionPane.ERROR_MESSAGE);
				}
			});
			return;
		}
	
		if(client.isStopped()) {
			client.start(WS_LOCATION);
		}
		
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			try {
				// Close other application
				try {
					pm.kill();
					Thread.sleep(400);
				} catch(Exception e2) {
					e2.printStackTrace();
				}
				
				// Set application type:
				
				this.client.getSocket().setApplication(appName);
				pm.setProcess(Runtime.getRuntime().exec(command));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
	
	/**
	 * 
	 * Check the condition BEFORE executing the web app
	 * 
	 * @return null if precondition is valid.
	 * 
	 */
	private final String checkRequirments() {
		if(appName.equals("ndex-save")) {
			if(appManager.getCurrentNetwork() == null) {
				
				return "Please select one of the networks from the collection before saving to NDEx.  It will be used for the thumbnail "
						+ "image when you save your network collection.";
			}
		}
		
		return null;
	}
	
}
