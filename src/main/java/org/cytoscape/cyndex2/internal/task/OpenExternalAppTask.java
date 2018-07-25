package org.cytoscape.cyndex2.internal.task;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.JSValue;
import com.teamdev.jxbrowser.chromium.events.ScriptContextAdapter;
import com.teamdev.jxbrowser.chromium.events.ScriptContextEvent;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;

/**
 * 
 * Task to execute a command and open a new external application as a new
 * process.
 *
 */

public class OpenExternalAppTask extends AbstractTask {

	// Name of the application
	private BrowserView browserView;
	private final String port;
	private final JDialog dialog;

	public OpenExternalAppTask(final JDialog dialog, final BrowserView browserView, String port) {
		this.port = port;
		this.dialog = dialog;
		this.browserView = browserView;
	}

	@Override
	public void run(TaskMonitor taskMonitor) {
		if (this.cancelled)
			return;
		// Open the CyNDEx-2 browser
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {

				try {
					if (dialog.getComponentCount() == 0) {
						throw new Exception("BrowserView was not added to the dialog");
					}
					Browser browser = browserView.getBrowser();

					browser.addScriptContextListener(new ScriptContextAdapter() {

						@Override
						public void onScriptContextCreated(ScriptContextEvent arg0) {
							JSValue window = browser.executeJavaScriptAndReturnValue("window");
							if (window != null) {
								window.asObject().setProperty("frame", dialog);
								window.asObject().setProperty("restPort", port);
							}
						}
					});
					if (!dialog.isVisible()) {
						browser.loadURL("http://cyndex.ndexbio.org/" + CyActivator.WEB_APP_VERSION
								+ "/index.html?cyrestport=" + port);
						dialog.setVisible(true);
					} else {
						dialog.toFront();
					}

					// Re-enable the search bar/toolbar components
				} catch (Exception e) {
					dialog.setVisible(false);
					System.out.println("Error loading CyNDEx2 browser: " + e.getMessage());

					JOptionPane.showMessageDialog(null,
							"An error occurred communicating with JxBrowser. Restart and try again.", "JxBrowser Error",
							JOptionPane.ERROR_MESSAGE);
					ExternalAppManager.setLoadFailed("Failed to load browser instance.\n" + e.getMessage());
				}

			}
		});

	}
}
