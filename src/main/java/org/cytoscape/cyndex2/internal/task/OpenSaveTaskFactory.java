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

public class OpenSaveTaskFactory extends AbstractTaskFactory {

	private final String appName;
	private final CyApplicationManager appManager;
	private final String saveType;
	private String port;
	
	
	private final JDialog dialog;
	

	public OpenSaveTaskFactory(final String saveType, final CyApplicationManager appManager, final CySwingApplication swingApp, final CyProperty<Properties> cyProps) {
		super();
		this.saveType = saveType;
		this.appName = ExternalAppManager.APP_NAME_SAVE;
		this.appManager = appManager;
		port = cyProps.getProperties().getProperty("rest.port");

		if (port == null)
			port = "1234";

		JFrame frame = swingApp.getJFrame();
		dialog = ExternalAppManager.getDialog(frame);
	}
	
	@Override
	public TaskIterator createTaskIterator() {
		// Store query info
		ExternalAppManager.appName = appName;
		ExternalAppManager.saveType = saveType;
		
		dialog.setSize(1000, 700);
		dialog.setLocationRelativeTo(dialog.getParent());

		TaskIterator ti = new TaskIterator();
		LoadBrowserTask loader = new LoadBrowserTask(dialog, ti);
		ti.append(loader);
		return ti;
	}

	@Override
	public boolean isReady() {
		if (ExternalAppManager.loadFailed())
			return false;

		return appManager.getCurrentNetwork() != null;
	}
}
