package org.cytoscape.fx.internal.task;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;

public class ShowMessageTaskFactory extends AbstractTaskFactory {

	private final CySwingApplication cySwingApplicationServiceRef;
	private final CyServiceRegistrar registrar;
	private final CyApplicationConfiguration appConfig;

	public ShowMessageTaskFactory(final CyServiceRegistrar registrar,
			final CySwingApplication cySwingApplicationServiceRef, final CyApplicationConfiguration appConfig) {
		this.cySwingApplicationServiceRef = cySwingApplicationServiceRef;
		this.registrar = registrar;
		this.appConfig = appConfig;
	}

	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new ShowMessageTask(registrar, cySwingApplicationServiceRef, appConfig));
	}
}