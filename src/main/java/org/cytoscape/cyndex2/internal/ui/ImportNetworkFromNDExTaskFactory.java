package org.cytoscape.cyndex2.internal.ui;

import org.cytoscape.cyndex2.internal.task.LoadBrowserTask;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class ImportNetworkFromNDExTaskFactory extends AbstractTaskFactory{    	

	@Override
	public TaskIterator createTaskIterator() {
		ExternalAppManager.query = "";
		ExternalAppManager.appName = ExternalAppManager.APP_NAME_LOAD;
		return new TaskIterator(new LoadBrowserTask());
	}

	@Override
	public boolean isReady() {
		return true;
	}

}