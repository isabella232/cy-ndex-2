package org.cytoscape.hybrid.internal.task;

import org.cytoscape.hybrid.internal.ws.ExternalAppManager;
import org.cytoscape.hybrid.internal.ws.WSClient;
import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;

public class NdexSaveTaskFactory extends AbstractNetworkViewTaskFactory {

	private final WSClient client;
	private final ExternalAppManager pm;
	private final String command;

	public NdexSaveTaskFactory(final WSClient client, final ExternalAppManager pm, String command) {
		this.client = client;
		this.pm = pm;
		this.command = command;
	}

	@Override
	public TaskIterator createTaskIterator(CyNetworkView networkView) {
		return new TaskIterator(new NdexSaveTask(networkView, client, pm, command));
	}
}
