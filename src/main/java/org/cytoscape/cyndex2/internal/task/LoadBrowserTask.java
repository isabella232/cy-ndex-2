package org.cytoscape.cyndex2.internal.task;

import java.awt.BorderLayout;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.CyActivator.BrowserCreationError;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

import com.teamdev.jxbrowser.chromium.swing.BrowserView;

public class LoadBrowserTask extends AbstractTask {
	private BrowserView browserView;
	private final String port;
	private final JDialog dialog;
	private TaskIterator ti;
	protected boolean complete = false;

	public LoadBrowserTask(final ExternalAppManager pm, final JDialog dialog, final TaskIterator ti) {
		this.ti = ti;
		this.dialog = dialog;
		this.port = pm.getPort();
	}

	@Override
	public void run(TaskMonitor taskMonitor) {
		// Load browserView and start external task, or show error message
		LoadBrowserTask task = this;
		Runnable runnable = new Runnable(){
			
			@Override
			public void run() {
				taskMonitor.setTitle("Loading CyNDEx-2 Browser");
				try {
					browserView = CyActivator.getBrowserView();
					
					if (browserView == null || browserView.getBrowser() == null)
						throw new BrowserCreationError("Browser failed to initialize.");

					taskMonitor.setProgress(.5);
					taskMonitor.setStatusMessage("Browser created, starting CyNDEx-2");

					if (browserView.getParent() == null)
						dialog.add(browserView, BorderLayout.CENTER);

					ti.insertTasksAfter(task, new OpenExternalAppTask(dialog, browserView, port));
				} catch (BrowserCreationError e) {
					taskMonitor.showMessage(TaskMonitor.Level.ERROR,
							"Failed to create browser instance for CyNDEx-2. Restart Cytoscape and try again.\nError: "
									+ e.getMessage());
					ti.append(new AbstractTask() {

						@Override
						public void run(TaskMonitor taskMonitor) throws Exception {
							SwingUtilities.invokeLater(new Runnable() {

								@Override
								public void run() {
									JOptionPane.showMessageDialog(null, e.getMessage());
								}

							});
						}

					});
					ExternalAppManager.setLoadFailed(e.getMessage());
				}
				complete = true;
			}
			
			
		};
		Thread thread = new Thread(runnable);
		
		thread.start();
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					Thread.sleep(20000);
					taskMonitor.setStatusMessage("CyNDEx2 is having trouble rendering the JXBrowser window. If the issue persists, try restarting Cytoscape.");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
		while (true){
			if (cancelled){
				thread.interrupt();
				break;
			}
			if (complete)
				break;
		}
	}
}
