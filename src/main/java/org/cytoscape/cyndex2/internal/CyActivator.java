package org.cytoscape.cyndex2.internal;

import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.ImageIcon;
import org.apache.commons.io.FileUtils;
import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractToolBarComponent;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.cyndex2.internal.rest.NdexClient;
import org.cytoscape.cyndex2.internal.rest.endpoints.NdexBaseResource;
import org.cytoscape.cyndex2.internal.rest.endpoints.NdexNetworkResource;
import org.cytoscape.cyndex2.internal.rest.endpoints.NdexStatusResource;
import org.cytoscape.cyndex2.internal.rest.endpoints.impl.NdexBaseResourceImpl;
import org.cytoscape.cyndex2.internal.rest.endpoints.impl.NdexNetworkResourceImpl;
import org.cytoscape.cyndex2.internal.rest.endpoints.impl.NdexStatusResourceImpl;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorBuilder;
import org.cytoscape.cyndex2.internal.rest.reader.LoadNetworkStreamTaskFactoryImpl;
import org.cytoscape.cyndex2.internal.singletons.CyObjectManager;
import org.cytoscape.cyndex2.internal.task.OpenBrowseTaskFactory;
import org.cytoscape.cyndex2.internal.task.OpenSaveTaskFactory;
import org.cytoscape.cyndex2.internal.ui.SaveNetworkToNDExToolbarComponent;
import org.cytoscape.cyndex2.internal.util.BrowserManager;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.cyndex2.server.StaticContentsServer;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.write.CyNetworkViewWriterFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNetworkTableManager;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.TaskFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CyActivator extends AbstractCyActivator {

	// Logger for this activator
	private static final Logger logger = LoggerFactory.getLogger(CyActivator.class);
	public static final String INSTALL_MAKER_FILE_NAME = "ndex-installed";
	private ExternalAppManager pm;
	private static final String STATIC_CONTENT_DIR = "cyndex-2";
	private static CyProperty<Properties> cyProps;
    private static String appVersion;
    private static String cytoscapeVersion;
    private static String appName;
    private static boolean hasCyNDEx1;
    private CIServiceManager ciServiceManager;
    
	private StaticContentsServer httpServer;

	public CyActivator() {
		super();
		hasCyNDEx1 = false;
	}
	
	
	
	
	@Override
	@SuppressWarnings("unchecked")
	public void start(BundleContext bc) throws InvalidSyntaxException {

	   for ( Bundle b : bc.getBundles()) {
//		   		System.out.println(b.getSymbolicName());
	        		if (b.getSymbolicName().equals("org.cytoscape.api-bundle")) {
	        			cytoscapeVersion = b.getVersion().toString();
	       // 			break;
	        		} else if (b.getSymbolicName().equals("org.cytoscape.ndex.cyNDEx")) {
/*	        			Version v = b.getVersion();
	        			System.out.println(v);
	        			int st = b.getState();
	        			System.out.println(st);
*/	        			
	        			hasCyNDEx1 = true;
	        		}
	    }
	   	Bundle currentBundle = bc.getBundle();
		appVersion = currentBundle.getVersion().toString();
		
		Dictionary d = currentBundle.getHeaders();
		appName = (String) d.get("Bundle-name");

		// Import dependencies
		final CyApplicationConfiguration config = getService(bc, CyApplicationConfiguration.class);

		final CyApplicationManager appManager = getService(bc, CyApplicationManager.class);

		cyProps = getService(bc, CyProperty.class, "(cyPropertyName=cytoscape3.props)");

		final CySwingApplication swingApp = getService(bc, CySwingApplication.class);
		final CySwingAppAdapter appAdapter = getService(bc, CySwingAppAdapter.class);
		final CyNetworkTableManager networkTableManager = getService(bc, CyNetworkTableManager.class);

		// Register these with the CyObjectManager singleton.
		CyObjectManager manager = CyObjectManager.INSTANCE;
		File configDir = config.getAppConfigurationDirectoryLocation(CyActivator.class);
		configDir.mkdirs();
		manager.setConfigDir(configDir);
		manager.setCySwingAppAdapter(appAdapter);
		manager.setNetworkTableManager(networkTableManager);

		pm = new ExternalAppManager();

		// For loading network
		final CxTaskFactoryManager tfManager = new CxTaskFactoryManager();
		registerServiceListener(bc, tfManager, "addReaderFactory", "removeReaderFactory", InputStreamTaskFactory.class);
		registerServiceListener(bc, tfManager, "addWriterFactory", "removeWriterFactory",
				CyNetworkViewWriterFactory.class);

		ciServiceManager = new CIServiceManager(bc);
		
		// For loading networks...
		final CyNetworkManager netmgr = getService(bc, CyNetworkManager.class);
		final CyNetworkViewManager networkViewManager = getService(bc, CyNetworkViewManager.class);
		final CyNetworkNaming cyNetworkNaming = getService(bc, CyNetworkNaming.class);
		final VisualMappingManager vmm = getService(bc, VisualMappingManager.class);
		final CyNetworkViewFactory nullNetworkViewFactory = getService(bc, CyNetworkViewFactory.class);
		final CyGroupFactory groupFactory = getService(bc, CyGroupFactory.class);
		final CyServiceRegistrar serviceRegistrar = getService(bc, CyServiceRegistrar.class);
		
		TaskFactory loadNetworkTF = new LoadNetworkStreamTaskFactoryImpl(netmgr, networkViewManager, cyProps,
				cyNetworkNaming, vmm, nullNetworkViewFactory, serviceRegistrar);

		// Start static content delivery server
		final File configRoot = config.getConfigurationDirectoryLocation();
		final String staticContentPath = configRoot.getAbsolutePath();

		// JXBrowser configuration
		BrowserManager.setConfigurationDirectory(new File(config.getConfigurationDirectoryLocation(), "jxbrowser"));
				
		// Create web app dir
		installWebApp(staticContentPath, bc);
		File staticPath = new File(staticContentPath, STATIC_CONTENT_DIR);
		startHttpServer(bc, staticPath.getAbsolutePath());

		// get QueryPanel icon
		ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource("images/ndex-logo.png"));

		// TF for NDEx Save Network
		final OpenSaveTaskFactory ndexSaveNetworkTaskFactory = new OpenSaveTaskFactory(ExternalAppManager.SAVE_NETWORK, appManager, pm, swingApp,
				cyProps);
		final Properties ndexSaveNetworkTaskFactoryProps = new Properties();

		ndexSaveNetworkTaskFactoryProps.setProperty(PREFERRED_MENU, "File.Export");
		ndexSaveNetworkTaskFactoryProps.setProperty(MENU_GRAVITY, "0.0");
		ndexSaveNetworkTaskFactoryProps.setProperty(TITLE, "Network to NDEx...");
		registerService(bc, ndexSaveNetworkTaskFactory, TaskFactory.class, ndexSaveNetworkTaskFactoryProps);

		// TF for NDEx Save Network
		final OpenSaveTaskFactory ndexSaveCollectionTaskFactory = new OpenSaveTaskFactory(ExternalAppManager.SAVE_COLLECTION, appManager, pm, swingApp,
				cyProps);
		final Properties ndexSaveCollectionTaskFactoryProps = new Properties();

		ndexSaveCollectionTaskFactoryProps.setProperty(PREFERRED_MENU, "File.Export");
		ndexSaveCollectionTaskFactoryProps.setProperty(MENU_GRAVITY, "0.1");
		ndexSaveCollectionTaskFactoryProps.setProperty(TITLE, "Collection to NDEx...");
		registerService(bc, ndexSaveCollectionTaskFactory, TaskFactory.class, ndexSaveCollectionTaskFactoryProps);

		// TF for NDEx save toolbar component
		SaveNetworkToNDExToolbarComponent toolbar = new SaveNetworkToNDExToolbarComponent();
		registerAllServices(bc, toolbar);
		
		// TF for NDEx Load
		final OpenBrowseTaskFactory ndexTaskFactory = new OpenBrowseTaskFactory(appManager, icon, pm, swingApp,
				cyProps);
		final Properties ndexTaskFactoryProps = new Properties();
//		ndexTaskFactoryProps.setProperty(IN_MENU_BAR, "false");
		ndexTaskFactoryProps.setProperty(PREFERRED_MENU, "File.Import.Network");
		ndexTaskFactoryProps.setProperty(MENU_GRAVITY, "0.0");
		ndexTaskFactoryProps.setProperty(TITLE, "NDEx...");

		registerAllServices(bc, ndexTaskFactory, ndexTaskFactoryProps);

		// Expose CyREST endpoints
		final ErrorBuilder errorBuilder = new ErrorBuilder(ciServiceManager, config);
		final NdexClient ndexClient = new NdexClient(errorBuilder);

		// Base
		registerService(bc, new NdexBaseResourceImpl(bc.getBundle().getVersion().toString(), errorBuilder, ciServiceManager),
				NdexBaseResource.class, new Properties());

		// Status
		registerService(bc, new NdexStatusResourceImpl(pm, errorBuilder, ciServiceManager), NdexStatusResource.class, new Properties());
		
		// Network IO
		registerService(bc, new NdexNetworkResourceImpl(ndexClient, errorBuilder, appManager, netmgr, ciServiceManager),
				 NdexNetworkResource.class, new Properties());

		// add Handler to remove networks from NetworkManager when deleted
		registerService(bc, new NdexNetworkAboutToBeDestroyedListener(), NetworkAboutToBeDestroyedListener.class,
				new Properties());	

	}

	
	private final void installWebApp(final String configDir, final BundleContext bc) {

		// This bundle's version
		final Version version = bc.getBundle().getVersion();

		if (!isInstalled(configDir, version.toString())) {
			final File webappDir = new File(configDir, STATIC_CONTENT_DIR);
			
			try {
				FileUtils.deleteDirectory(webappDir);
				BrowserManager.clearCache();
				webappDir.mkdir();
				extractWebapp(bc.getBundle(), STATIC_CONTENT_DIR, configDir);
				File markerFile = new File(webappDir, INSTALL_MAKER_FILE_NAME + "-" + version.toString() + ".txt");
				markerFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private final static boolean isInstalled(final String configDir, final String bundleVersion) {
		// This is the indicator of installation.

		final File cyndexDir = new File(configDir, STATIC_CONTENT_DIR);
		if (!cyndexDir.exists()) {
			return false;
		}
		final String markerFileName = INSTALL_MAKER_FILE_NAME + "-" + bundleVersion + ".txt";
		final File markerFile = new File(cyndexDir, markerFileName);

		if (markerFile.exists()) {
			// Exact match required. Otherwise, simply override the existing
			// contents.
			return true;
		}

		return false;
	}

	private final void extractWebapp(final Bundle bundle, final String path, final String targetDir) {
		Enumeration<String> ress = bundle.getEntryPaths(path);

		while (ress != null && ress.hasMoreElements()) {
			String fileName = ress.nextElement();

			File f = new File(targetDir, fileName);
			if (fileName.endsWith("/")) {
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

	private final static void copyEntry(final InputStream zis, final String filePath) throws IOException {
		final byte[] buffer = new byte[4096];
		try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
			int read = 0;
			while ((read = zis.read(buffer)) != -1) {
				bos.write(buffer, 0, read);
			}
		}
	}

	private final static boolean checkPort(final int port) {
		try (Socket sock = new Socket("localhost", port)) {
			return false;
		} catch (IOException ex) {
			return true;
		}
	}

	private final void startHttpServer(BundleContext bc, String path) {

		logger.info("CyNDEx-2 web application root directory: " + path);
		if (!checkPort(2222)) {
			ExternalAppManager.setLoadFailed("Port 2222 is not available");
			return;
		}

		httpServer = new StaticContentsServer(path);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			try {
				httpServer.startServer();
			} catch (Exception e) {
				ExternalAppManager.setLoadFailed("Failed to start local server.\n" + e.getMessage());
				throw new RuntimeException("Could not start Static Content server in separate thread.", e);
			}
		});
	}

	@Override
	public void shutDown() {
		logger.info("Shutting down CyNDEx-2...");

		try {
			httpServer.stopServer();
		} catch (Exception e) {
			logger.debug("Failed to stop server. Did it ever start?\n" + e.getMessage());
		}

		BrowserManager.clearCache();
		if (ciServiceManager != null){
						ciServiceManager.close();
		}
		super.shutDown();
	}
	
    public static String getAppVersion() {return appVersion;}
    public static String getCyVersion() { return cytoscapeVersion;}
    public static String getAppName () { return appName;}

	public static boolean hasCyNDEx1() {
		return hasCyNDEx1;
	}

	public static void setHasCyNDEX1(boolean hasCyNDEx1) {
		CyActivator.hasCyNDEx1 = hasCyNDEx1;
	}


}