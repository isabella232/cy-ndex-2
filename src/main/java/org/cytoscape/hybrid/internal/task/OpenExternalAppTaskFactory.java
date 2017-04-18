package org.cytoscape.hybrid.internal.task;

import org.cytoscape.hybrid.internal.ws.ExternalAppManager;
import org.cytoscape.hybrid.internal.ws.WSClient;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class OpenExternalAppTaskFactory extends AbstractTaskFactory {

	private final String appName;
	private final WSClient client;
	private final ExternalAppManager pm;
	private final String command;

	public OpenExternalAppTaskFactory(final String appName, final WSClient client, 
			final ExternalAppManager pm, String command) {
		this.client = client;
		this.pm = pm;
		this.command = command;
		this.appName = appName;
	}

	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new OpenExternalAppTask(appName, client, pm, command));
	}
}
