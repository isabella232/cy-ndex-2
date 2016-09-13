package org.cytoscape.hybrid.internal;

import static org.cytoscape.application.swing.ActionEnableSupport.ENABLE_FOR_NETWORK;
import static org.cytoscape.work.ServiceProperties.ENABLE_FOR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.awt.Dimension;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.ActionEnableSupport;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.hybrid.events.WSHandler;
import org.cytoscape.hybrid.internal.electron.NativeAppInstaller;
import org.cytoscape.hybrid.internal.login.LoginManager;
import org.cytoscape.hybrid.internal.login.NdexLoginMessageHandler;
import org.cytoscape.hybrid.internal.task.OpenExternalAppTaskFactory;
import org.cytoscape.hybrid.internal.ui.NdexPanel;
import org.cytoscape.hybrid.internal.ws.ExternalAppManager;
import org.cytoscape.hybrid.internal.ws.SaveMessageHandler;
import org.cytoscape.hybrid.internal.ws.WSClient;
import org.cytoscape.hybrid.internal.ws.WSServer;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.AbstractCyActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.annotation.JsonAppend.Prop;

public class CyActivator extends AbstractCyActivator {

	private static final Logger logger = LoggerFactory.getLogger(CyActivator.class);

	public CyActivator() {
		super();
	}

	public void start(BundleContext bc) {
		// Importing Service
		final CySwingApplication desktop = getService(bc, CySwingApplication.class);
		final CyApplicationConfiguration config = getService(bc, CyApplicationConfiguration.class);
		final CyApplicationManager appManager = getService(bc, CyApplicationManager.class);
		final CyEventHelper eventHelper = getService(bc, CyEventHelper.class);
		final CyRootNetworkManager rootManager = getService(bc, CyRootNetworkManager.class);
		@SuppressWarnings("unchecked")
		final CyProperty<Properties> cyProp = getService(bc,CyProperty.class,"(cyPropertyName=cytoscape3.props)");

		// Local components
		final LoginManager loginManager = new LoginManager();
		final ExternalAppManager pm = new ExternalAppManager();
		final WSClient client = new WSClient(desktop, pm, eventHelper, loginManager, cyProp);
		final NativeAppInstaller installer = new NativeAppInstaller(config);

		// This is a singleton
		final NdexPanel panel = new NdexPanel(installer, pm, client, loginManager);
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
		});

		// Menu item for NDEx Save
		final OpenExternalAppTaskFactory ndexSaveTaskFactory = new OpenExternalAppTaskFactory("ndex-save", client, pm, installer.getCommand());
		final OpenExternalAppTaskFactory ndexLoginTaskFactory = new OpenExternalAppTaskFactory("ndex-login", client, pm, installer.getCommand());
		
		final Properties ndexSaveTaskFactoryProps = new Properties();
		ndexSaveTaskFactoryProps.setProperty(ENABLE_FOR, ActionEnableSupport.ENABLE_FOR_NETWORK);
		ndexSaveTaskFactoryProps.setProperty(PREFERRED_MENU, "File.Export");
		ndexSaveTaskFactoryProps.setProperty(MENU_GRAVITY, "1000.0");
		ndexSaveTaskFactoryProps.setProperty(TITLE, "Network Collection to NDEx");
		registerAllServices(bc, ndexSaveTaskFactory, ndexSaveTaskFactoryProps);
		
		final Properties ndexLoginTaskFactoryProps = new Properties();
		ndexLoginTaskFactoryProps.setProperty(PREFERRED_MENU, "Tools");
		ndexLoginTaskFactoryProps.setProperty(MENU_GRAVITY, "1000.0");
		ndexLoginTaskFactoryProps.setProperty(TITLE, "NDEx: Login / Logout");
		registerAllServices(bc, ndexLoginTaskFactory, ndexLoginTaskFactoryProps);
	
		// WebSocket event handlers
		
		final WSHandler saveHandler = new SaveMessageHandler(appManager, loginManager, rootManager, cyProp);
		final WSHandler loginHandler = new NdexLoginMessageHandler(loginManager);
		client.getSocket().addHandler(saveHandler);
		client.getSocket().addHandler(loginHandler);
	}

	@Override
	public void shutDown() {
		logger.info("Shutting down NDEx Valet...");
	}
}