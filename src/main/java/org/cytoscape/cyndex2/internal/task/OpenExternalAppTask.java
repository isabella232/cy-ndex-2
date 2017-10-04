package org.cytoscape.cyndex2.internal.task;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.application.swing.CySwingApplication;
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
	private final CySwingApplication swingApp;
	private static JDialog dialog = null;

	public OpenExternalAppTask(final BrowserView browserView,
			final CySwingApplication swingApp, String port) {
		this.swingApp = swingApp;
		this.port = port;
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
				JFrame frame = swingApp.getJFrame();
				if (dialog == null)
					dialog = new JDialog(frame, "CyNDEx2 Browser", ModalityType.APPLICATION_MODAL);
				
				dialog.getModalityType();
				dialog.setSize(1000, 700);
				dialog.setLocationRelativeTo(null);
				dialog.add(browserView, BorderLayout.CENTER);
				
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
				try{
					browserView.requestFocusInWindow();
					browser.loadURL("http://localhost:2222");
					dialog.setAlwaysOnTop(false);
					dialog.setVisible(true);
				}catch (Exception e){
					dialog.setVisible(false);
					System.out.println("Error loading CyNDEx2 browser: " + e.getMessage());
					JOptionPane.showMessageDialog(swingApp.getJFrame(), "An error occurred communicating with JxBrowser. Restart and try again.", "JxBrowser Error", JOptionPane.ERROR_MESSAGE);
					OpenExternalAppTaskFactory.setLoadFailed();
				}
				
			}
		});

	}
}
