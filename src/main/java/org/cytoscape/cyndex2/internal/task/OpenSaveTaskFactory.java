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
	private final ExternalAppManager pm;
	private final CyApplicationManager appManager;
	private String port;
	
	private final JDialog dialog;
	

	public OpenSaveTaskFactory(final CyApplicationManager appManager,
			final ExternalAppManager pm, final CySwingApplication swingApp, final CyProperty<Properties> cyProps) {
		super();
		this.appName = ExternalAppManager.APP_NAME_SAVE;
		this.appManager = appManager;
		this.pm = pm;
		port = cyProps.getProperties().getProperty("rest.port");

		if (port == null)
			port = "1234";

		JFrame frame = swingApp.getJFrame();
		dialog = pm.getDialog(frame);
		
	}
	
	@Override
	public TaskIterator createTaskIterator() {
		// Store query info
		pm.setAppName(appName);
		pm.setPort(port);

		dialog.setSize(1000, 700);
		dialog.setLocationRelativeTo(null);

		TaskIterator ti = new TaskIterator();
		LoadBrowserTask loader = new LoadBrowserTask(pm, dialog, ti);
		ti.append(loader);
		return ti;
	}

	@Override
	public boolean isReady() {
		if (ExternalAppManager.loadFailed)
			return false;

		return appManager.getCurrentNetwork() != null;
	}
}
