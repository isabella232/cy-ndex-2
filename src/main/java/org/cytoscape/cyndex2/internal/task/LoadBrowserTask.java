package org.cytoscape.cyndex2.internal.task;

import java.awt.BorderLayout;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.cyndex2.errors.BrowserCreationError;
import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.util.BrowserManager;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.cyndex2.internal.util.StringResources.LoadBrowserStage;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import com.teamdev.jxbrowser.chromium.swing.BrowserView;

public class LoadBrowserTask extends AbstractTask {
	private BrowserView browserView;
	private JDialog dialog;
	protected boolean complete = false;

	public LoadBrowserTask() {

		dialog = CyActivator.getDialog();
		if (!dialog.isVisible()) {
			dialog.setSize(1000, 700);
		}
		dialog.setLocationRelativeTo(null);
		
		// give warnings if cyNDEX1 is found.
		if (CyActivator.hasCyNDEx1()) {
			JOptionPane.showMessageDialog(dialog,
					"We have detected you have both the CyNDEx and CyNDEx-2 apps installed and ENABLED.\n"
							+ "We recommend you DISABLE one of the two apps or you might run into compatibility "
							+ "issues in Cytoscape.",
					"Warning", JOptionPane.WARNING_MESSAGE);
			CyActivator.setHasCyNDEX1(false);
		}

	}

	@Override
	public void run(TaskMonitor taskMonitor) {

		// Load browserView and start external task, or show error message
		LoadBrowserTask task = this;
		Runnable runnable = new Runnable() {

			@Override
			public void run() {

				taskMonitor.setTitle("Loading CyNDEx-2");
				try {
					browserView = BrowserManager.getBrowserView(taskMonitor);

					taskMonitor.setTitle("Starting CyNDEx-2");
					if (browserView == null || browserView.getBrowser() == null)
						throw new BrowserCreationError("Browser failed to initialize.");

					taskMonitor.setTitle("Loading CyNDEx-2");
					LoadBrowserStage.STARTING_BROWSER.updateTaskMonitor(taskMonitor);

					if (browserView.getParent() == null)
						dialog.add(browserView, BorderLayout.CENTER);

					getTaskIterator().insertTasksAfter(task,
							new OpenExternalAppTask(dialog, browserView, CyActivator.getCyRESTPort()));
				} catch (BrowserCreationError e) {
					taskMonitor.showMessage(TaskMonitor.Level.ERROR,
							"Failed to create browser instance for CyNDEx-2. Restart Cytoscape and try again.\nError: "
									+ e.getMessage());
					getTaskIterator().append(new AbstractTask() {

						@Override
						public void run(TaskMonitor taskMonitorParameter) throws Exception {
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
					Thread.sleep(40000);
					taskMonitor.setStatusMessage(
							"CyNDEx2 is having trouble rendering the JXBrowser window. If the issue persists, try restarting Cytoscape.");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
		while (true) {
			if (cancelled) {
				thread.interrupt();
				break;
			}
			if (complete)
				break;
		}
	}
}
