package org.cytoscape.fx.internal.task;

import java.util.Properties;

import javax.swing.SwingUtilities;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.fx.internal.ui.WebViewPanel;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShowMessageTask extends AbstractTask {

	private static final Logger logger = LoggerFactory.getLogger(ShowMessageTask.class);

	private final NetworkTaskFactory fitContent;
	private final CySwingApplication cySwingApplicationServiceRef;

	private final CyServiceRegistrar registrar;


	public ShowMessageTask(final CyServiceRegistrar registrar, final CySwingApplication cySwingApplicationServiceRef, final NetworkTaskFactory fitContent) {
		this.fitContent = fitContent;
		this.cySwingApplicationServiceRef = cySwingApplicationServiceRef;
		this.registrar = registrar;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		logger.warn("FX test code start");
		
		final WebViewPanel panel = new WebViewPanel(fitContent);

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				panel.load();
				registrar.registerAllServices(panel, new Properties());
			}
		});
		logger.warn("FX test code end");
	}

}
