package org.cytoscape.cyndex2.internal.task;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Dialog.ModalityType;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

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
	private final String appName;
	private final ExternalAppManager pm;
	private final JDialog dialog;
	private BrowserView browserView;
	private final String port;

	public OpenExternalAppTask(final String appName, final ExternalAppManager pm, final JDialog dialog, String port) {
		this.appName = appName;
		this.pm = pm;
		this.dialog = dialog;
		this.port = port;
	}

	private JPanel getProgressBarPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		JLabel label = new JLabel("Loading browser...");
		label.setAlignmentX(JTextField.CENTER_ALIGNMENT);
		panel.add(label);
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		JProgressBar progressBar = new JProgressBar();
		progressBar.setValue(50);
		panel.setMinimumSize(new Dimension(400, 10));
		progressBar.setIndeterminate(true);
		panel.add(progressBar);
		return panel;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {

		if (pm.loadFailed()) {
			System.out.println("LOAD FAILED");
			return;
		}
		pm.setAppName(appName);

		final ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			try {
				if (pm.loadSuccess()) {
					browserView = pm.getBrowserView();
				} else {
					JPanel panel = getProgressBarPanel();
					dialog.add(panel, BorderLayout.CENTER);
					dialog.pack();
					dialog.setResizable(false);
					dialog.setLocationRelativeTo(null);
					dialog.setVisible(true);
					browserView = pm.getBrowserView();

					if (browserView == null) {
						dialog.setVisible(false);
						return;
					}
					dialog.remove(panel);
					dialog.setVisible(false);
					dialog.add(browserView, BorderLayout.CENTER);

					
				}
				dialog.setModalityType(ModalityType.APPLICATION_MODAL);
				if (dialog.getModalityType() != ModalityType.APPLICATION_MODAL){
					System.out.println("Modality not set correctly");
				}
				
				dialog.setSize(1000, 700);
				dialog.setLocationRelativeTo(null);
				

				browserView.getBrowser().addScriptContextListener(new ScriptContextAdapter() {

					@Override
					public void onScriptContextCreated(ScriptContextEvent arg0) {
						JSValue window = browserView.getBrowser().executeJavaScriptAndReturnValue("window");
						if (window != null) {
							window.asObject().setProperty("frame", dialog);
							window.asObject().setProperty("restPort", port);
						}

					}
				});

				browserView.getBrowser().loadURL("http://localhost:2222");

				dialog.setVisible(true);
			} catch (IllegalStateException e) {
				System.out.println("Failed to load URL");
				dialog.setVisible(false);

			} catch (Exception e) {
				if (dialog != null)
					dialog.setVisible(false);
				e.printStackTrace();
				throw new RuntimeException("Could not start the application: " + appName, e);
			}
		});

	}
}
