package org.cytoscape.cyndex2.internal.ui;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.cyndex2.internal.rest.parameter.SaveParameters;
import org.cytoscape.cyndex2.internal.task.OpenDialogTaskFactory;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.TaskIterator;

public class SaveNetworkToNDExTaskFactory extends OpenDialogTaskFactory {
	private final CyApplicationManager appManager;
	public SaveNetworkToNDExTaskFactory(CyApplicationManager appManager, String appName) {
		super(appName);
		this.appManager = appManager;
	}

	CyNetwork net;
	
	@Override
	public TaskIterator createTaskIterator() {
		SaveParameters.INSTANCE.saveType = ExternalAppManager.SAVE_NETWORK;
		return super.createTaskIterator();
	}
	
	@Override
	public boolean isReady() {
		return super.isReady() && appManager.getCurrentNetwork() != null;
	}


}