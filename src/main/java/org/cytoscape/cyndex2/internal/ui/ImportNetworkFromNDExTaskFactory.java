package org.cytoscape.cyndex2.internal.ui;

import org.cytoscape.cyndex2.internal.task.OpenDialogTaskFactory;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.work.TaskIterator;

public class ImportNetworkFromNDExTaskFactory extends OpenDialogTaskFactory{

	public ImportNetworkFromNDExTaskFactory(String appName) {
		super(appName);
	}
	
	@Override
	public TaskIterator createTaskIterator() {
		ExternalAppManager.query = "";
		return super.createTaskIterator();
	}

	
}