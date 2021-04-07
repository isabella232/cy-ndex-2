package org.cytoscape.cyndex2.internal.ui;

import org.cytoscape.cyndex2.internal.rest.parameter.LoadParameters;
import org.cytoscape.cyndex2.internal.task.OpenDialogTaskFactory;
import org.cytoscape.work.TaskIterator;

public class ImportNetworkFromNDExTaskFactory extends OpenDialogTaskFactory{

	public ImportNetworkFromNDExTaskFactory(String appName) {
		super(appName);
	}
	
	@Override
	public TaskIterator createTaskIterator() {
		LoadParameters.INSTANCE.searchTerm = "";
		LoadParameters.INSTANCE.userNetworksOnly = false;
		return super.createTaskIterator();
	}

	
}