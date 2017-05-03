package org.cytoscape.hybrid.internal;

import static org.cytoscape.work.ServiceProperties.ENABLE_FOR;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JToolBar;
import javax.swing.border.EmptyBorder;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.ActionEnableSupport;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.ci.CIErrorFactory;
import org.cytoscape.ci.CIExceptionFactory;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.hybrid.internal.electron.NativeAppInstaller;
import org.cytoscape.hybrid.internal.rest.NdexClient;
import org.cytoscape.hybrid.internal.rest.NdexImportResource;
import org.cytoscape.hybrid.internal.rest.NdexImportResourceImpl;
import org.cytoscape.hybrid.internal.rest.NdexStatusResource;
import org.cytoscape.hybrid.internal.rest.NdexStatusResourceImpl;
import org.cytoscape.hybrid.internal.rest.errors.ErrorBuilder;
import org.cytoscape.hybrid.internal.rest.reader.LoadNetworkStreamTaskFactoryImpl;
import org.cytoscape.hybrid.internal.task.OpenExternalAppTaskFactory;
import org.cytoscape.hybrid.internal.ui.SearchBox;
import org.cytoscape.hybrid.internal.ws.ExternalAppManager;
import org.cytoscape.hybrid.internal.ws.WSClient;
import org.cytoscape.hybrid.internal.ws.WSServer;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.write.CyNetworkViewWriterFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskManager;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CyActivator extends AbstractCyActivator {

	// Logger for this activator
	private static final Logger logger = LoggerFactory.getLogger(CyActivator.class);

	private static final Dimension PANEL_SIZE = new Dimension(400, 40);
	private static final Dimension PANEL_SIZE_MAX = new Dimension(900, 100);

	private WSServer server;

	private JToolBar toolBar;
	private JPanel panel;

	public CyActivator() {
		super();
	}

	public void start(BundleContext bc) {

		// Import dependencies
		final CySwingApplication desktop = getService(bc, CySwingApplication.class);
		final CyApplicationConfiguration config = getService(bc, CyApplicationConfiguration.class);
		final CyApplicationManager appManager = getService(bc, CyApplicationManager.class);
		final CyEventHelper eventHelper = getService(bc, CyEventHelper.class);
		final TaskManager<?, ?> tm = getService(bc, TaskManager.class);

		@SuppressWarnings("unchecked")
		final CyProperty<Properties> cyProp = getService(bc, CyProperty.class, "(cyPropertyName=cytoscape3.props)");

		// For loading network
		final CxTaskFactoryManager tfManager = new CxTaskFactoryManager();
		registerServiceListener(bc, tfManager, "addReaderFactory", "removeReaderFactory", InputStreamTaskFactory.class);
		registerServiceListener(bc, tfManager, "addWriterFactory", "removeWriterFactory",
				CyNetworkViewWriterFactory.class);

		// CI Error handlers
		CIExceptionFactory ciExceptionFactory = this.getService(bc, CIExceptionFactory.class);
		CIErrorFactory ciErrorFactory = this.getService(bc, CIErrorFactory.class);

		// For loading networks...
		final CyNetworkManager netmgr = getService(bc, CyNetworkManager.class);
		final CyNetworkViewManager networkViewManager = getService(bc, CyNetworkViewManager.class);
		final CyNetworkNaming cyNetworkNaming = getService(bc, CyNetworkNaming.class);
		final VisualMappingManager vmm = getService(bc, VisualMappingManager.class);
		final CyNetworkViewFactory nullNetworkViewFactory = getService(bc, CyNetworkViewFactory.class);
		final CyServiceRegistrar serviceRegistrar = getService(bc, CyServiceRegistrar.class);
		TaskFactory loadNetworkTF = new LoadNetworkStreamTaskFactoryImpl(netmgr, networkViewManager, cyProp,
				cyNetworkNaming, vmm, nullNetworkViewFactory, serviceRegistrar);

		// Start WS server
		this.startServer(bc);

		// Initialize OSGi services
		final ExternalAppManager pm = new ExternalAppManager();
		final WSClient client = new WSClient(desktop, pm, eventHelper, cyProp);

		final NativeAppInstaller installer = createInstaller(desktop, config);

		// TF for NDEx Save
		final OpenExternalAppTaskFactory ndexSaveTaskFactory = new OpenExternalAppTaskFactory("ndex-save", client, pm,
				installer.getCommand());
		final Properties ndexSaveTaskFactoryProps = new Properties();
		ndexSaveTaskFactoryProps.setProperty(ENABLE_FOR, ActionEnableSupport.ENABLE_FOR_NETWORK);
		ndexSaveTaskFactoryProps.setProperty(PREFERRED_MENU, "File.Export");
		ndexSaveTaskFactoryProps.setProperty(MENU_GRAVITY, "0.0");
		ndexSaveTaskFactoryProps.setProperty(TITLE, "Network Collection to NDEx...");
		registerAllServices(bc, ndexSaveTaskFactory, ndexSaveTaskFactoryProps);

		// TF for NDEx Load
		final OpenExternalAppTaskFactory ndexTaskFactory = new OpenExternalAppTaskFactory("ndex", client, pm,
				installer.getCommand());
		final Properties ndexTaskFactoryProps = new Properties();
		ndexTaskFactoryProps.setProperty(IN_MENU_BAR, "false");
		registerAllServices(bc, ndexTaskFactory, ndexTaskFactoryProps);

		// Export
		installer.executeInstaller(new SearchBox(pm, ndexTaskFactory, tm));

		// Expose CyREST endpoints
		final ErrorBuilder errorBuilder = new ErrorBuilder(ciErrorFactory);
		final NdexClient ndexClient = new NdexClient(errorBuilder);
		registerService(bc, new NdexStatusResourceImpl(), NdexStatusResource.class, new Properties());
		registerService(bc, new NdexImportResourceImpl(ndexClient, errorBuilder, appManager, netmgr, tfManager,
				loadNetworkTF, ciExceptionFactory, ciErrorFactory), NdexImportResource.class, new Properties());
	}

	private final void startServer(BundleContext bc) {
		// Start server
		this.server = new WSServer();
		registerAllServices(bc, server, new Properties());
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			try {
				server.start();
			} catch (Exception e) {
				throw new RuntimeException("Could not start WS server in separate thread.", e);
			}
		});
	}

	private final NativeAppInstaller createInstaller(final CySwingApplication desktop,
			final CyApplicationConfiguration config) {
		final JProgressBar bar = new JProgressBar();
		bar.setValue(0);
		JPanel progress = new JPanel();
		progress.setBorder(new EmptyBorder(10, 10, 10, 10));
		progress.setPreferredSize(PANEL_SIZE);
		progress.setSize(PANEL_SIZE);
		progress.setMaximumSize(PANEL_SIZE_MAX);
		progress.setBackground(new Color(245, 245, 245));

		JLabel label = new JLabel("Loading NDEx App: ");
		progress.setLayout(new BorderLayout());
		progress.add(bar, BorderLayout.CENTER);
		progress.add(label, BorderLayout.WEST);
		toolBar = desktop.getJToolBar();

		// This is the actual installer extracting binaries
		return new NativeAppInstaller(config, bar, progress, toolBar, desktop);
	}

	@Override
	public void shutDown() {
		logger.info("Shutting down NDEx Valet...");
		server.stop();
		toolBar.remove(panel);
		panel = null;
		server = null;
	}
}