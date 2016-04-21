package org.cytoscape.fx.internal.task;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class ShowMessageTaskFactory extends AbstractTaskFactory {

	private final NetworkTaskFactory fitContent;
	private final CySwingApplication cySwingApplicationServiceRef;
	private final CyServiceRegistrar registrar;

	public ShowMessageTaskFactory(final CyServiceRegistrar registrar,
			final CySwingApplication cySwingApplicationServiceRef, final NetworkTaskFactory fitContent) {
		this.fitContent = fitContent;
		this.cySwingApplicationServiceRef = cySwingApplicationServiceRef;
		this.registrar = registrar;
	}

	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ShowMessageTask(registrar, cySwingApplicationServiceRef, fitContent));
	}
}