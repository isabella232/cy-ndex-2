package org.cytoscape.cyndex2.internal.task;

import java.util.Collection;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.cyndex2.external.SaveParameters;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.task.NetworkCollectionTaskFactory;
import org.cytoscape.work.TaskIterator;

public class OpenSaveTaskFactory extends OpenDialogTaskFactory implements NetworkCollectionTaskFactory {
	private final CyApplicationManager appManager;
	
	public OpenSaveTaskFactory(final CyApplicationManager appManager) {
		super(ExternalAppManager.APP_NAME_SAVE);
		this.appManager = appManager;
	}
	
	@Override
	public TaskIterator createTaskIterator() {
		return createTaskIterator(appManager.getCurrentNetwork());
	}

	@Override
	public boolean isReady() {
		if (ExternalAppManager.loadFailed())
			return false;

		return appManager.getCurrentNetwork() != null;
	}

	@Override
	public TaskIterator createTaskIterator(Collection<CyNetwork> nets) {
		if (nets.isEmpty()) {
			return null;
		}
		
		return createTaskIterator(nets.iterator().next());
	}

	@Override
	public boolean isReady(Collection<CyNetwork> nets) {
		return isReady() && nets.size() == 1;
	}
	
	private TaskIterator createTaskIterator(CyNetwork net) {
		SaveParameters.INSTANCE.suid = net.getSUID();
		SaveParameters.INSTANCE.saveType = ExternalAppManager.SAVE_NETWORK;
		return super.createTaskIterator();
		
	}
}
