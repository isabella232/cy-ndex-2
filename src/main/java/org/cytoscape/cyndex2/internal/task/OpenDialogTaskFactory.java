package org.cytoscape.cyndex2.internal.task;

import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class OpenDialogTaskFactory extends AbstractTaskFactory {

	protected final String appName;

	public OpenDialogTaskFactory(final String appName) {
		super();
		this.appName = appName;

	}

	@Override
	public TaskIterator createTaskIterator() {
		TaskIterator ti = new TaskIterator();

		if (ExternalAppManager.busy)
			return ti;

		// Store query info
		ExternalAppManager.appName = appName;
		ExternalAppManager.busy = true;

		LoadBrowserTask loader = new LoadBrowserTask();
		ti.append(loader);

		return ti;
	}

	@Override
	public boolean isReady() {
		return !ExternalAppManager.busy && !ExternalAppManager.loadFailed();
	}

}
