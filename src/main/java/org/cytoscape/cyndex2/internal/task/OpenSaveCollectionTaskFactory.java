package org.cytoscape.cyndex2.internal.task;

import java.util.Collection;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.cyndex2.internal.rest.parameter.SaveParameters;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.task.RootNetworkCollectionTaskFactory;
import org.cytoscape.work.TaskIterator;

public class OpenSaveCollectionTaskFactory extends OpenDialogTaskFactory implements RootNetworkCollectionTaskFactory {
	private final CyApplicationManager appManager;
	
	public OpenSaveCollectionTaskFactory(final CyApplicationManager appManager) {
		super(ExternalAppManager.APP_NAME_SAVE);
		this.appManager = appManager;
	}
	
	@Override
	public TaskIterator createTaskIterator() {
		CyNetwork net = appManager.getCurrentNetwork();
		if (net == null) {
			return null;
		}
		CyRootNetwork root = ((CySubNetwork) net).getRootNetwork();
		return createTaskIterator(root);
	}

	@Override
	public boolean isReady() {
		if (ExternalAppManager.loadFailed())
			return false;

		return appManager.getCurrentNetwork() != null;
	}

	@Override
	public TaskIterator createTaskIterator(Collection<CyRootNetwork> roots) {
		if (roots.isEmpty()) {
			return null;
		}
		return createTaskIterator(roots.iterator().next());
	}

	@Override
	public boolean isReady(Collection<CyRootNetwork> roots) {
		return !ExternalAppManager.loadFailed() && roots.size() == 1;
	}
	
	private TaskIterator createTaskIterator(CyRootNetwork root) {
		SaveParameters.suid = root.getSUID();
		SaveParameters.INSTANCE.saveType = ExternalAppManager.SAVE_COLLECTION;
		return super.createTaskIterator();
		
	}
}
