package org.cytoscape.hybrid.internal;

import java.awt.Dimension;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.hybrid.internal.electron.InstallNativeApps;
import org.cytoscape.hybrid.internal.ui.NdexPanel;
import org.cytoscape.hybrid.internal.ws.ExternalAppManager;
import org.cytoscape.hybrid.internal.ws.WSClient;
import org.cytoscape.hybrid.internal.ws.WSServer;
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
		// Importing Service
		final CySwingApplication desktop = getService(bc, CySwingApplication.class);
		final CyApplicationConfiguration config = getService(bc, CyApplicationConfiguration.class);
		final CyEventHelper eventHelper = getService(bc, CyEventHelper.class);

		// Local components
		final ExternalAppManager pm = new ExternalAppManager();
		final WSClient client = new WSClient(desktop, pm, eventHelper);
		final InstallNativeApps installer = new InstallNativeApps(config);

		// This is a singleton
		final NdexPanel panel = new NdexPanel(installer, pm, client);
		registerAllServices(bc, panel, new Properties());

		final CytoPanel parent = desktop.getCytoPanel(CytoPanelName.SOUTH_WEST);
		panel.setMinimumSize(new Dimension(600, 600));
		panel.setPreferredSize(new Dimension(600, 1000));
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