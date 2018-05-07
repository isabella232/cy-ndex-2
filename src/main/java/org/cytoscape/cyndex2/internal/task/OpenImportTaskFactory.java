package org.cytoscape.cyndex2.internal.task;

import java.util.Properties;

import javax.swing.JDialog;
import javax.swing.JFrame;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.property.CyProperty;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class OpenImportTaskFactory extends AbstractTaskFactory {

	private JFrame parentFrame;	

	public OpenImportTaskFactory(final CySwingApplication swingApp) {
		super();

		parentFrame = swingApp.getJFrame();
	}
	
	@Override
	public TaskIterator createTaskIterator() {
		// Store query info

		TaskIterator ti = new TaskIterator();
		ImportFromNDExTask loader = new ImportFromNDExTask(parentFrame);
		ti.append(loader);
		return ti;
	}

	@Override
	public boolean isReady() {
		if (ExternalAppManager.loadFailed)
			return false;

		return true;
	}
}
