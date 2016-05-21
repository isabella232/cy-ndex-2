package org.cytoscape.fx.internal.task;

import java.awt.Dimension;
import java.io.IOException;
import java.util.Properties;

import javax.swing.SwingUtilities;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.fx.internal.ui.WebViewPanel;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShowPanelTask extends AbstractTask {

	private static final Logger logger = LoggerFactory.getLogger(ShowPanelTask.class);

	private final CySwingApplication cySwingApplicationServiceRef;

	private final CyServiceRegistrar registrar;

	private final String location;

	private static final Integer DEF_PANEL_WIDTH = 1200;
	private static final Integer DEF_PANEL_HEIGHT = 400;

	public ShowPanelTask(final CyServiceRegistrar registrar, final CySwingApplication cySwingApplicationServiceRef,
			final CyApplicationConfiguration appConfig) {

		this.cySwingApplicationServiceRef = cySwingApplicationServiceRef;
		this.registrar = registrar;

		final UIResourceGenerator resourceGenerator = new UIResourceGenerator(appConfig);

		try {
			resourceGenerator.extractPreviewTemplate();
		} catch (IOException e) {
			throw new IllegalStateException("Could not extract UI template", e);
		}

		this.location = resourceGenerator.getResourceLocation();
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		String targetIndexFile = "file://" + this.location + "/index.html";
		System.out.println(targetIndexFile);
		final WebViewPanel panel = new WebViewPanel(targetIndexFile);

		final Dimension size = new Dimension(DEF_PANEL_WIDTH, DEF_PANEL_HEIGHT);

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				panel.setSize(size);
				panel.setPreferredSize(size);

				// Load the actual content of the page
				panel.loadPage();
				registrar.registerAllServices(panel, new Properties());

				// Make the panel selected
				final CytoPanel westPanel = cySwingApplicationServiceRef.getCytoPanel(CytoPanelName.WEST);

				westPanel.getThisComponent().setPreferredSize(size);
				westPanel.getThisComponent().setSize(size);

				final int panelIdx = westPanel.indexOfComponent(panel);
				westPanel.setSelectedIndex(panelIdx);
			}
		});
	}
}
