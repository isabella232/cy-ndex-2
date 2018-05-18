package org.cytoscape.cyndex2.internal.task;

import javax.swing.JDialog;
import javax.swing.JFrame;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class OpenDialogTaskFactory extends AbstractTaskFactory {

	protected final String appName;
	protected final JDialog dialog;

	public OpenDialogTaskFactory(final String appName, final CySwingApplication swingApp) {
		super();
		this.appName = appName;

		JFrame frame = swingApp.getJFrame();
		dialog = ExternalAppManager.getDialog(frame);
	}

	@Override
	public TaskIterator createTaskIterator() {
		TaskIterator ti = new TaskIterator();

		if (ExternalAppManager.busy)
			return ti;

		// Store query info
		ExternalAppManager.appName = appName;
		ExternalAppManager.busy = true;

		dialog.setSize(1000, 700);
		dialog.setLocationRelativeTo(null);

		LoadBrowserTask loader = new LoadBrowserTask(dialog, ti);
		ti.append(loader);

		return ti;
	}

	@Override
	public boolean isReady() {
		return !ExternalAppManager.busy && !ExternalAppManager.loadFailed();
	}

}
