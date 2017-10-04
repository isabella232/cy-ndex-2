package org.cytoscape.cyndex2.internal.task;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.CyActivator.BrowserCreationError;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;

public class LoadBrowserTask extends AbstractTask {
	private BrowserView browserView;
	private final CySwingApplication swingApp;
	private final String port;
	private TaskIterator ti;

	public LoadBrowserTask(final ExternalAppManager pm, final TaskIterator ti, final CySwingApplication swingApp) {
		this.ti = ti;
		this.swingApp = swingApp;
		this.port = pm.getPort();
	}

	private BrowserView getBrowserView() throws BrowserCreationError {
		// return the non-null browserView object or throw a
		// BrowserCreationException
		if (!OpenExternalAppTaskFactory.loadFailed()) {
			Browser browser = CyActivator.getBrowser();
			browserView = new BrowserView(browser);
		}
		if (browserView == null)
			throw new BrowserCreationError("Browser failed to initialize.");
		return browserView;
	}

	@Override
	public void run(TaskMonitor taskMonitor) {
		// Load browserView and start external task, or show error message

		taskMonitor.setTitle("Loading CyNDEx-2 Browser");
		try {
			browserView = getBrowserView();

			taskMonitor.setProgress(.5);
			taskMonitor.setStatusMessage("Browser created, starting CyNDEx-2");
			ti.insertTasksAfter(this, new OpenExternalAppTask(browserView, swingApp, port));
		} catch (BrowserCreationError e) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR,
					"Failed to create browser instance for CyNDEx-2. Restart Cytoscape and try again.\nError: " + e.getMessage());
			ti.append(new AbstractTask(){

				@Override
				public void run(TaskMonitor taskMonitor) throws Exception {
					SwingUtilities.invokeLater(new Runnable(){

						@Override
						public void run() {
							JOptionPane.showMessageDialog(null, e.getMessage());	
						}
						
					});
				}
				
			});
			OpenExternalAppTaskFactory.setLoadFailed();
		}
	}
}
