package org.cytoscape.hybrid.internal;

import static org.cytoscape.work.ServiceProperties.ENABLE_FOR;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
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
import org.cytoscape.ci_bridge_impl.CIProvider;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.hybrid.events.ExternalAppClosedEventListener;
import org.cytoscape.hybrid.events.ExternalAppStartedEventListener;
import org.cytoscape.hybrid.internal.electron.NativeAppInstaller;
import org.cytoscape.hybrid.internal.electron.StaticContentsServer;
import org.cytoscape.hybrid.internal.rest.NdexClient;
import org.cytoscape.hybrid.internal.rest.endpoints.NdexBaseResource;
import org.cytoscape.hybrid.internal.rest.endpoints.NdexNetworkResource;
import org.cytoscape.hybrid.internal.rest.endpoints.NdexStatusResource;
import org.cytoscape.hybrid.internal.rest.endpoints.impl.NdexBaseResourceImpl;
import org.cytoscape.hybrid.internal.rest.endpoints.impl.NdexNetworkResourceImpl;
import org.cytoscape.hybrid.internal.rest.endpoints.impl.NdexStatusResourceImpl;
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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.deser.Deserializers.Base;

public class CyActivator extends AbstractCyActivator {
	
	// Logger for this activator
	private static final Logger logger = LoggerFactory.getLogger(CyActivator.class);

	private static final Dimension PANEL_SIZE = new Dimension(400, 40);
	private static final Dimension PANEL_SIZE_MAX = new Dimension(900, 100);

	private static final String CDN_SERVER_URL_TAG = "cyndex.cdn.url";
	private final String BASE_URL = "http://chianti.ucsd.edu/~kono/ci/app/cyndex2";
	
	private static final String STATIC_CONTENT_DIR = "cyndex-2";
	
	private WSServer server;

	private JToolBar toolBar;
	private JPanel panel;
	
	private String cdnHost;
	private String cdnUrl;

	public CyActivator() {
		super();
	}
	
	private final void setURL(final CyProperty<Properties> cyProp) {
		// Get remote file location
		final Object url = cyProp.getProperties().get(CDN_SERVER_URL_TAG);
		cdnUrl = BASE_URL;
		
		if(url != null) {
			cdnUrl = url.toString();
		}
		
		if(!cdnUrl.endsWith("/")) {
			cdnUrl = cdnUrl + "/";
		}
		
		try {
			URL urlObj = new URL(cdnUrl);
			cdnHost = urlObj.getHost();
		} catch (MalformedURLException e) {
			throw new RuntimeException("Invalid URL for the installer location.", e);
		}
		
		System.out.println("* CDN HOST: " + cdnHost);
		System.out.println("* CDN URL: " + cdnUrl);
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
		
		// Create native package (Electron code) locations from properties
		setURL(cyProp);
		
		// For loading network
		final CxTaskFactoryManager tfManager = new CxTaskFactoryManager();
		registerServiceListener(bc, tfManager, "addReaderFactory", "removeReaderFactory", InputStreamTaskFactory.class);
		registerServiceListener(bc, tfManager, "addWriterFactory", "removeWriterFactory",
				CyNetworkViewWriterFactory.class);

		// CI Error handlers
//		CIExceptionFactory ciExceptionFactory = this.getService(bc, CIExceptionFactory.class);
//		CIErrorFactory ciErrorFactory = this.getService(bc, CIErrorFactory.class);
		
		CIExceptionFactory ciExceptionFactory = CIProvider.getCIExceptionFactory();
		CIErrorFactory ciErrorFactory = null;
		try {
			ciErrorFactory = CIProvider.getCIErrorFactory(bc);
		} catch (IOException e) {
			throw new RuntimeException("Could not create CIErrorFactory.", e);
		}
		
		if(ciErrorFactory == null) {
			throw new RuntimeException("Could not create CIErrorFactory.");
		}

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
		
		// Start static content delivery server
		final File configRoot = config.getConfigurationDirectoryLocation();
		final String staticContentPath = configRoot.getAbsolutePath();
		
		// Create web app dir
		installWebApp(staticContentPath, bc);
		File staticPath = new File(staticContentPath, STATIC_CONTENT_DIR);
		this.startHttpServer(bc, staticPath.getAbsolutePath());

		// Initialize OSGi services
		final ExternalAppManager pm = new ExternalAppManager();
		final WSClient client = new WSClient(desktop, pm, eventHelper, cyProp);

		final NativeAppInstaller installer = createInstaller(desktop, config, bc.getBundle().getVersion().toString());
		
		// TF for NDEx Save
		final OpenExternalAppTaskFactory ndexSaveTaskFactory = new OpenExternalAppTaskFactory(ExternalAppManager.APP_NAME_SAVE, client, pm,
				installer.getCommand(), cyProp, eventHelper, appManager);
		final Properties ndexSaveTaskFactoryProps = new Properties();
		ndexSaveTaskFactoryProps.setProperty(ENABLE_FOR, ActionEnableSupport.ENABLE_FOR_NETWORK);
		ndexSaveTaskFactoryProps.setProperty(PREFERRED_MENU, "File.Export");
		ndexSaveTaskFactoryProps.setProperty(MENU_GRAVITY, "0.0");
		ndexSaveTaskFactoryProps.setProperty(TITLE, "Network Collection to NDEx...");
		registerAllServices(bc, ndexSaveTaskFactory, ndexSaveTaskFactoryProps);

		// TF for NDEx Load
		final OpenExternalAppTaskFactory ndexTaskFactory = new OpenExternalAppTaskFactory(ExternalAppManager.APP_NAME_LOAD, client, pm,
				installer.getCommand(), cyProp, eventHelper, appManager);
		final Properties ndexTaskFactoryProps = new Properties();
		ndexTaskFactoryProps.setProperty(IN_MENU_BAR, "false");
		registerAllServices(bc, ndexTaskFactory, ndexTaskFactoryProps);

		// Export
		final SearchBox searchPanel = new SearchBox(pm, ndexTaskFactory, tm);
		registerService(bc, searchPanel, ExternalAppClosedEventListener.class);
		registerService(bc, searchPanel, ExternalAppStartedEventListener.class);
		installer.executeInstaller(searchPanel);

		// Expose CyREST endpoints
		final ErrorBuilder errorBuilder = new ErrorBuilder(ciErrorFactory, ciExceptionFactory, config);
		final NdexClient ndexClient = new NdexClient(errorBuilder);
		
		// Status endpoint

		// Base
		registerService(bc, new NdexBaseResourceImpl(bc.getBundle().getVersion().toString(), errorBuilder), NdexBaseResource.class, new Properties());
		
		// Status
		registerService(bc, new NdexStatusResourceImpl(pm, errorBuilder, appManager), NdexStatusResource.class, new Properties());
		
		// Network IO
		registerService(bc, new NdexNetworkResourceImpl(ndexClient, errorBuilder, appManager, netmgr, tfManager,
				loadNetworkTF, ciExceptionFactory, ciErrorFactory), NdexNetworkResource.class, new Properties());
	}
	
	private final void installWebApp(final String configDir, final BundleContext bc) {
		
		// This bundle's version
		final Version version = bc.getBundle().getVersion();
		
		if(!isInstalled(configDir, version.toString())) {
			final File webappDir = new File(configDir, STATIC_CONTENT_DIR);
			webappDir.mkdir();
			extractWebapp(bc.getBundle(), STATIC_CONTENT_DIR, configDir);
		}
	}
	
	private final boolean isInstalled(final String configDir, final String bundleVersion) {
		// This is the indicator of installation.
		final String markerFileName = NativeAppInstaller.INSTALL_MAKER_FILE_NAME + "-" + bundleVersion + ".txt";
		final File markerFile = new File(configDir, markerFileName);
		
		if(markerFile.exists()) {
			// Exact match required.  Otherwise, simply override the existing contents.
			return true;
		}
		
		return false;
	}
	
	private final void extractWebapp(final Bundle bundle, final String path, String targetDir) {
		Enumeration<String> ress = bundle.getEntryPaths(path);
		
		while(ress.hasMoreElements()) {
			String fileName = ress.nextElement();
			
			File f = new File(targetDir, fileName);
			if(fileName.endsWith("/")) {
				f.mkdir();
				extractWebapp(bundle, fileName, targetDir);
			} else {
				// Extract
				try {
					copyEntry(bundle.getEntry(fileName).openStream(), f.getAbsolutePath());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private final void copyEntry(final InputStream zis, final String filePath) throws IOException {
		final byte[] buffer = new byte[4096];
		final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
		int read = 0;
		while ((read = zis.read(buffer)) != -1) {
			bos.write(buffer, 0, read);
		}
		bos.close();
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
	private final void startHttpServer(BundleContext bc, String path) {
		
		System.out.println("Web app root = " + path);
		final StaticContentsServer httpServer = new StaticContentsServer(path);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			try {
				httpServer.startServer();
			} catch (Exception e) {
				throw new RuntimeException("Could not start Static Content server in separate thread.", e);
			}
		});
	}

	private final NativeAppInstaller createInstaller(final CySwingApplication desktop,
			final CyApplicationConfiguration config, final String version) {
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
		return new NativeAppInstaller(config, bar, progress, toolBar, desktop, version, cdnHost, cdnUrl);
	}

	@Override
	public void shutDown() {
		logger.info("Shutting down NDEx Valet...");
		server.stop();
		if(toolBar != null && panel != null) {
			toolBar.remove(panel);
		}
		panel = null;
		server = null;
	}
}