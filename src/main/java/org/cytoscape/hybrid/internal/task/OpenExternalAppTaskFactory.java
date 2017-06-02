package org.cytoscape.hybrid.internal.task;

import java.util.Properties;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.hybrid.internal.ws.ExternalAppManager;
import org.cytoscape.hybrid.internal.ws.WSClient;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.property.CyProperty;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class OpenExternalAppTaskFactory extends AbstractTaskFactory {

	private final String appName;
	private final WSClient client;
	private final ExternalAppManager pm;
	private final String command;
	private final CyProperty<Properties> props;
	private final CyEventHelper eventHelper;
	
	private final CyApplicationManager appManager;

	
	public OpenExternalAppTaskFactory(final String appName, final WSClient client, 
			final ExternalAppManager pm, String command, final CyProperty<Properties> props, 
			final CyEventHelper eventHelper, final CyApplicationManager appManager) {
		this.client = client;
		this.pm = pm;
		this.command = command;
		this.appName = appName;
		this.props = props;
		this.eventHelper = eventHelper;
		this.appManager = appManager;
	}

	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new OpenExternalAppTask(appName, client, pm, command, props, eventHelper));
	}
	
	@Override
	public boolean isReady() {
		
		if(appName == ExternalAppManager.APP_NAME_SAVE) {
			final CyNetwork curNetwork = appManager.getCurrentNetwork();
			if(curNetwork == null) {
				return false;
			} else {
				return true;
			}
		} else {
			return true;
		}
	}
}
