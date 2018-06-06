package org.cytoscape.cyndex2.internal.ui;

import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.cyndex2.internal.task.LoadBrowserTask;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class SaveNetworkToNDExTaskFactory extends AbstractTaskFactory implements SetCurrentNetworkListener {
	CyNetwork net;
	
	@Override
	public TaskIterator createTaskIterator() {
		ExternalAppManager.saveType = ExternalAppManager.SAVE_NETWORK;
		ExternalAppManager.query = "";
		ExternalAppManager.appName = ExternalAppManager.APP_NAME_SAVE;
		return new TaskIterator(new LoadBrowserTask());
	}
	
	@Override
	public boolean isReady() {
		return net != null;
	}

	@Override
	public void handleEvent(SetCurrentNetworkEvent arg0) {
		net = arg0.getNetwork();
	}

}