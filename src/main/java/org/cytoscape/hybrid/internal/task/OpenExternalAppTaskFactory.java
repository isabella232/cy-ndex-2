package org.cytoscape.hybrid.internal.task;

import java.util.Properties;

import org.cytoscape.hybrid.internal.ws.ExternalAppManager;
import org.cytoscape.hybrid.internal.ws.WSClient;
import org.cytoscape.property.CyProperty;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class OpenExternalAppTaskFactory extends AbstractTaskFactory {

	private final String appName;
	private final WSClient client;
	private final ExternalAppManager pm;
	private final String command;
	private final CyProperty<Properties> props;

	public OpenExternalAppTaskFactory(final String appName, final WSClient client, 
			final ExternalAppManager pm, String command, final CyProperty<Properties> props) {
		this.client = client;
		this.pm = pm;
		this.command = command;
		this.appName = appName;
		this.props = props;
	}

	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new OpenExternalAppTask(appName, client, pm, command, props));
	}
}
