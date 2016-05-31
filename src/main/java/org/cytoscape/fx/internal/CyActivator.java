package org.cytoscape.fx.internal;

import java.awt.Dimension;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.fx.internal.ui.NdexPanel;
import org.cytoscape.fx.internal.ws.ExternalAppManager;
import org.cytoscape.fx.internal.ws.WSServer;
import org.cytoscape.service.util.AbstractCyActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CyActivator extends AbstractCyActivator {

	private static final Logger logger = LoggerFactory.getLogger(CyActivator.class);

	public CyActivator() {
		super();
	}

	public void start(BundleContext bc) {
		final CySwingApplication cySwingApplicationServiceRef = getService(bc, CySwingApplication.class);
		final ExternalAppManager pm = new ExternalAppManager();

		// This is a singleton
		final NdexPanel panel = new NdexPanel(cySwingApplicationServiceRef, pm);
		registerAllServices(bc, panel, new Properties());

		final CytoPanel parent = cySwingApplicationServiceRef.getCytoPanel(CytoPanelName.SOUTH_WEST);
		panel.setMinimumSize(new Dimension(600, 600));
		panel.setPreferredSize(new Dimension(600, 1000));
		panel.setSize(new Dimension(600, 1000));
		parent.setState(CytoPanelState.DOCK);
		parent.setSelectedIndex(parent.indexOfComponent(panel));

		// Start server
		final WSServer server = new WSServer();
		registerAllServices(bc, server, new Properties());
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			try {
				server.start();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Server started ");
		});
	}

	@Override
	public void shutDown() {
		logger.info("Shutting down NDEx Valet...");
	}
}