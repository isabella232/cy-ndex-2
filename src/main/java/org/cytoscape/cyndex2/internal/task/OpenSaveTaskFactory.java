package org.cytoscape.cyndex2.internal.task;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.work.TaskIterator;

public class OpenSaveTaskFactory extends OpenDialogTaskFactory {
	private final String saveType;	
	private final CyApplicationManager appManager;
	
	public OpenSaveTaskFactory(final String saveType, final CySwingApplication swingApp, final CyApplicationManager appManager) {
		super(ExternalAppManager.APP_NAME_SAVE, swingApp);
		this.saveType = saveType;
		this.appManager = appManager;
	}
	
	@Override
	public TaskIterator createTaskIterator() {
		ExternalAppManager.saveType = saveType;
		return super.createTaskIterator();
	}

	@Override
	public boolean isReady() {
		if (ExternalAppManager.loadFailed())
			return false;

		return appManager.getCurrentNetwork() != null;
	}
}
