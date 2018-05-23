package org.cytoscape.cyndex2.internal.task;

import java.util.Collection;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.task.NetworkViewCollectionTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;

public class OpenSaveTaskFactory extends OpenDialogTaskFactory implements NetworkViewCollectionTaskFactory {
	private final String saveType;	
	private final CyApplicationManager appManager;
	
	public OpenSaveTaskFactory(final String saveType, final CyApplicationManager appManager) {
		super(ExternalAppManager.APP_NAME_SAVE);
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

	@Override
	public TaskIterator createTaskIterator(Collection<CyNetworkView> arg0) {
		return createTaskIterator();
	}

	@Override
	public boolean isReady(Collection<CyNetworkView> arg0) {
		return isReady();
	}
}
