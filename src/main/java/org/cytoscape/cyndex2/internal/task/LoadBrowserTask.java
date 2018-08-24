package org.cytoscape.cyndex2.internal.task;

import java.awt.BorderLayout;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.errors.BrowserCreationError;
import org.cytoscape.cyndex2.internal.util.BrowserManager;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.cyndex2.internal.util.StringResources.LoadBrowserStage;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import com.teamdev.jxbrowser.chromium.swing.BrowserView;

public class LoadBrowserTask extends AbstractTask {
	private BrowserView browserView;
	private final JDialog dialog;
	protected boolean complete = false;
	protected String loadError;

	public LoadBrowserTask(JDialog dialog) {
		this.dialog = dialog;

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

	public void showMessage(String s) {
		getTaskIterator().append(new AbstractTask() {

			@Override
			public void run(TaskMonitor taskMonitorParameter) throws Exception {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						JOptionPane.showMessageDialog(null, s);
					}

				});
			}

		});
	}

	@Override
	public void run(TaskMonitor taskMonitor) {
		if (dialog == null) {
			getTaskIterator().insertTasksAfter(LoadBrowserTask.this,
					new OpenExternalAppTask(CyActivator.getCyRESTPort()));
			return;
		}
		loadError = null;
		Thread loadThread = new Thread(new Runnable() {

			@Override
			public void run() {

				try {
					browserView = BrowserManager.getBrowserView(taskMonitor);
					if (browserView == null || browserView.getBrowser() == null) {
						throw new BrowserCreationError("Failed to initialize browser instance.");
					}
					LoadBrowserStage.STARTING_BROWSER.updateTaskMonitor(taskMonitor);
					if (browserView.getParent() == null) {
						dialog.add(browserView, BorderLayout.CENTER);
					}
				} catch (BrowserCreationError e) {
					loadError = e.getMessage();
				}
			}

		});
		loadThread.start();

		long t = System.currentTimeMillis();
		long end = t + 40000;
		try {
			while (loadThread.isAlive()) {
				if (cancelled) {
					loadThread.interrupt();
					return;
				}
				if (System.currentTimeMillis() > end) {
					loadError = "Loading the browser is taking too long.";
					break;
				}

				Thread.sleep(200);
			}
			
		} catch (InterruptedException e) {

		}

		if (loadError == null) {
			getTaskIterator().insertTasksAfter(LoadBrowserTask.this,
					new OpenExternalAppTask(dialog, browserView, CyActivator.getCyRESTPort()));
		} else {
			showMessage("An error occurred while creating the browser for CyNDEx2: " + loadError
					+ " Try restarting Cytoscape.\n\n"
					+ "If the issue persists, try setting the `cyndex2.defaultBrowser` property to `true`.");
		}
	}

	public void run2(TaskMonitor taskMonitor) {

		// Load browserView and start external task, or show error message
		Thread loadThread = new Thread(new Runnable() {

			@Override
			public void run() {

				taskMonitor.setTitle("Loading CyNDEx-2 Browser");

				if (dialog == null) {
					getTaskIterator().insertTasksAfter(LoadBrowserTask.this,
							new OpenExternalAppTask(CyActivator.getCyRESTPort()));
				} else {

					try {
						browserView = BrowserManager.getBrowserView(taskMonitor);
						if (browserView == null || browserView.getBrowser() == null) {
							throw new BrowserCreationError("Browser failed to initialize.");
						}

						taskMonitor.setStatusMessage("Opening browser");
						LoadBrowserStage.STARTING_BROWSER.updateTaskMonitor(taskMonitor);

						if (browserView.getParent() == null) {
							dialog.add(browserView, BorderLayout.CENTER);
						}

						getTaskIterator().insertTasksAfter(LoadBrowserTask.this,
								new OpenExternalAppTask(dialog, browserView, CyActivator.getCyRESTPort()));
					} catch (BrowserCreationError e) {
						taskMonitor.showMessage(TaskMonitor.Level.ERROR,
								"Failed to create browser instance for CyNDEx-2. Restart Cytoscape and try again.\nError: "
										+ e.getMessage());
						showMessage(e.getMessage());
						ExternalAppManager.setLoadFailed(e.getMessage());
					}
				}
			}

		});
		loadThread.start();

		Thread waiter = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(40000);
					cancelled = true;
					taskMonitor.setStatusMessage(
							"CyNDEx2 is having trouble rendering the JXBrowser window. If the issue persists, try restarting Cytoscape.");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});

		waiter.start();

		while (true) {
			if (cancelled) {
				loadThread.interrupt();
				break;
			}
			if (complete) {
				if (dialog != null) {
					dialog.toFront();
				}
				break;
			}
		}
		waiter.interrupt();
	}
}
