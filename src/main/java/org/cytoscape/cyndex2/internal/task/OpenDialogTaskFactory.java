package org.cytoscape.cyndex2.internal.task;

import java.awt.Dialog.ModalityType;

import javax.swing.JDialog;

import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class OpenDialogTaskFactory extends AbstractTaskFactory {

	protected final String appName;
	private static JDialog dialog;

	public OpenDialogTaskFactory(final String appName) {
		super();
		this.appName = appName;

	}

	private static JDialog getDialog() {
		if (CyActivator.useDefaultBrowser()) {
			return null;
		}
		
		if (dialog == null) {
			dialog = new JDialog(null, "CyNDEx2 Browser", ModalityType.MODELESS);
			dialog.setAlwaysOnTop(true);
			if (!dialog.isVisible()) {
				dialog.setSize(1000, 700);
				dialog.setLocationRelativeTo(null);
			}
		}
		return dialog;
	}

	@Override
	public TaskIterator createTaskIterator() {
		TaskIterator ti = new TaskIterator();

		// Store query info
		ExternalAppManager.appName = appName;

		LoadBrowserTask loader = new LoadBrowserTask(getDialog());
		ti.append(loader);

		return ti;
	}

	@Override
	public boolean isReady() {
		return !ExternalAppManager.loadFailed();
	}

}
