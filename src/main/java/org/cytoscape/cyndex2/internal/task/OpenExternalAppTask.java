package org.cytoscape.cyndex2.internal.task;

import java.awt.Desktop;
import java.net.URI;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.cyndex2.external.SaveParameters;
import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger logger = LoggerFactory.getLogger(CyActivator.class);
	// Name of the application
	private BrowserView browserView;
	private final String port;
	private final JDialog dialog;

	public OpenExternalAppTask(final JDialog dialog, final BrowserView browserView, String port) {
		this.port = port;
		this.dialog = dialog;
		this.browserView = browserView;
	}

	public OpenExternalAppTask(String port) {
		this.port = port;
		this.dialog = null;
		this.browserView = null;
	}

	private Browser initBrowser() throws Exception {
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
		return browser;
	}

	@Override
	public void run(TaskMonitor taskMonitor) {
		if (this.cancelled)
			return;

		StringBuilder urlStr = new StringBuilder();
		urlStr.append("http://cyndex.ndexbio.org/");
		urlStr.append(CyActivator.WEB_APP_VERSION);
		urlStr.append("/index.html?cyrestport=");
		urlStr.append(port);

		if (ExternalAppManager.appName.equals(ExternalAppManager.APP_NAME_SAVE)) {
			urlStr.append("&suid=" + String.valueOf(SaveParameters.INSTANCE.suid));
		}

		final String url = urlStr.toString();

		// Open the CyNDEx-2 browser
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					if (dialog == null) {
						if (Desktop.isDesktopSupported()) {
							Desktop.getDesktop().browse(new URI(url));
						} else {
							throw new Exception("Unable to open the default browser from a Java application.");
						}
					} else {
						Browser browser = initBrowser();
						browser.loadURL(url);

						dialog.setVisible(true);
						dialog.toFront();
					}
					// Re-enable the search bar/toolbar components
				} catch (Exception e) {

					final boolean isGetPeerError = e.getMessage().equals(
							"java.lang.NoSuchMethodException: com.teamdev.jxbrowser.chromium.swing.internal.LightWeightWidget.getPeer()")
							|| e.getMessage().equals("java.lang.NoSuchMethodException: javax.swing.JDialog.getPeer()");
					if (isGetPeerError) {
						logger.warn("Ignored getPeer() error.");
						try {
							dialog.setVisible(true);
							dialog.toFront();
						} catch (Exception e2) {
							if (dialog != null) {
								dialog.setVisible(false);
							}

							JOptionPane.showMessageDialog(null, "Unable to load the CyNDEx2 browser: " + e.getMessage(),
									"JxBrowser Error", JOptionPane.ERROR_MESSAGE);
							logger.error("Unable to load the CyNDEx2 browser.", e2);

						}
					} else {
						if (dialog != null) {
							dialog.setVisible(false);
						}

						JOptionPane.showMessageDialog(null, "Unable to load the CyNDEx2 browser: " + e.getMessage(),
								"JxBrowser Error", JOptionPane.ERROR_MESSAGE);
						logger.error("Unable to load the CyNDEx2 browser.", e);
					}

//					ExternalAppManager.setLoadFailed("Failed to load browser instance.\n" + e.getMessage());
				}

			}
		});

	}
}
