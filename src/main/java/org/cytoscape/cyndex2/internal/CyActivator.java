package org.cytoscape.cyndex2.internal;

import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.ci.CIErrorFactory;
import org.cytoscape.ci.CIExceptionFactory;
import org.cytoscape.ci_bridge_impl.CIProvider;
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
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.cyndex2.server.StaticContentsServer;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.write.CyNetworkViewWriterFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNetworkTableManager;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.util.swing.LookAndFeelUtil;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.TaskFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.BrowserContext;
import com.teamdev.jxbrowser.chromium.BrowserContextParams;
import com.teamdev.jxbrowser.chromium.BrowserPreferences;
import com.teamdev.jxbrowser.chromium.BrowserType;
import com.teamdev.jxbrowser.chromium.PopupContainer;
import com.teamdev.jxbrowser.chromium.PopupHandler;
import com.teamdev.jxbrowser.chromium.PopupParams;
import com.teamdev.jxbrowser.chromium.events.DisposeEvent;
import com.teamdev.jxbrowser.chromium.events.DisposeListener;
import com.teamdev.jxbrowser.chromium.events.LoadAdapter;
import com.teamdev.jxbrowser.chromium.events.LoadEvent;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;

public class CyActivator extends AbstractCyActivator {

	// Logger for this activator
	private static final Logger logger = LoggerFactory.getLogger(CyActivator.class);
	public static final String INSTALL_MAKER_FILE_NAME = "ndex-installed";
	private ExternalAppManager pm;
	private static final String STATIC_CONTENT_DIR = "cyndex-2";
	private static Browser browser;
	private static BrowserView browserView;
	private static CyProperty<Properties> cyProps;
	private static File jxbrowserConfigLocation;

	private StaticContentsServer httpServer;

	public CyActivator() {
		super();
	}

	public static class BrowserCreationError extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 4687535679654703495L;

		public BrowserCreationError(String message) {
			super("JXBrowser instance creation failed.\n" + message);
		}
	}

	private static File[] getLockFiles() {
		// return whether libjxbrowser dylib/dll exists, which would cause
		// Browser instantiation to fail
		File[] instances = jxbrowserConfigLocation.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				// return name.startsWith("libjxbrowser-common64-") &&
				// name.endsWith(".dylib");
				return name.endsWith("_0.lock");
			}
		});
		return instances;
	}

	public static BrowserView getBrowserView() throws BrowserCreationError {
		// returns non-null Browser object or an Exception

		if (browser == null) {

			if (LookAndFeelUtil.isMac() && getLockFiles().length > 0) {
				throw new BrowserCreationError(
						"A .lock file exists in CytoscapeConfiguration/jxbrowser. The JXBrowser process must already be running.");
			}

			// Uncomment for development port
			BrowserPreferences.setChromiumSwitches("--remote-debugging-port=9222", "--ipc-connection-timeout=2");
			// BrowserPreferences.setChromiumSwitches("--ipc-connection-timeout=2");

			// Create the binary in the CytoscapeConfig
			System.setProperty("jxbrowser.chromium.dir", jxbrowserConfigLocation.getAbsolutePath());

			try {

				BrowserContextParams params = new BrowserContextParams(jxbrowserConfigLocation.getAbsolutePath());
				BrowserContext context = new BrowserContext(params);
				browser = new Browser(BrowserType.LIGHTWEIGHT, context);

			} catch (Exception e) {
				throw new BrowserCreationError(e.getMessage());
			}

			if (browser == null) {
				throw new BrowserCreationError("Browser failed to initialize.");
			}

			// Enable local storage and popups
			BrowserPreferences preferences = browser.getPreferences();
			preferences.setLocalStorageEnabled(true);
			browser.setPreferences(preferences);
			browser.addLoadListener(new LoadAdapter() {
				@Override
				public void onDocumentLoadedInMainFrame(LoadEvent event) {
					Browser browser = event.getBrowser();
					browser.executeJavaScript("localStorage");
				}
			});

			browser.setPopupHandler(new CustomPopupHandler());
			browserView = new BrowserView(browser);

		}

		return browserView;
	}

	@SuppressWarnings("unchecked")
	public void start(BundleContext bc) {

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

		// CI Error handlers
		// CIExceptionFactory ciExceptionFactory = this.getService(bc,
		// CIExceptionFactory.class);
		// CIErrorFactory ciErrorFactory = this.getService(bc,
		// CIErrorFactory.class);

		CIExceptionFactory ciExceptionFactory = CIProvider.getCIExceptionFactory();

		CIErrorFactory ciErrorFactory = null;
		try {
			ciErrorFactory = CIProvider.getCIErrorFactory(bc);
		} catch (IOException e) {
			throw new RuntimeException("Could not create CIErrorFactory.", e);
		}

		if (ciErrorFactory == null) {
			throw new RuntimeException("Could not create CIErrorFactory.");
		}

		// For loading networks...
		final CyNetworkManager netmgr = getService(bc, CyNetworkManager.class);
		final CyNetworkViewManager networkViewManager = getService(bc, CyNetworkViewManager.class);
		final CyNetworkNaming cyNetworkNaming = getService(bc, CyNetworkNaming.class);
		final VisualMappingManager vmm = getService(bc, VisualMappingManager.class);
		final CyNetworkViewFactory nullNetworkViewFactory = getService(bc, CyNetworkViewFactory.class);
		final CyServiceRegistrar serviceRegistrar = getService(bc, CyServiceRegistrar.class);
		TaskFactory loadNetworkTF = new LoadNetworkStreamTaskFactoryImpl(netmgr, networkViewManager, cyProps,
				cyNetworkNaming, vmm, nullNetworkViewFactory, serviceRegistrar);

		// Start static content delivery server
		final File configRoot = config.getConfigurationDirectoryLocation();
		final String staticContentPath = configRoot.getAbsolutePath();

		// Create web app dir
		installWebApp(staticContentPath, bc);
		File staticPath = new File(staticContentPath, STATIC_CONTENT_DIR);
		startHttpServer(bc, staticPath.getAbsolutePath());

		// get QueryPanel icon
		ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource("images/ndex-logo.png"));

		// JXBrowser configuration
		jxbrowserConfigLocation = new File(config.getConfigurationDirectoryLocation(), "jxbrowser");

		if (!jxbrowserConfigLocation.exists())
			try {
				jxbrowserConfigLocation.mkdir();
			} catch (SecurityException e) {
				ExternalAppManager.setLoadFailed("Failed to create JXBrowser directory in CytoscapeConfiguration");
			}
		BrowserPreferences.setChromiumDir(jxbrowserConfigLocation.getAbsolutePath());
		System.setProperty(BrowserPreferences.TEMP_DIR_PROPERTY, jxbrowserConfigLocation.getAbsolutePath());
		System.setProperty(BrowserPreferences.CHROMIUM_DIR_PROPERTY, jxbrowserConfigLocation.getAbsolutePath());
		System.setProperty(BrowserPreferences.USER_AGENT_PROPERTY, jxbrowserConfigLocation.getAbsolutePath());

		// TF for NDEx Save
		final OpenSaveTaskFactory ndexSaveNetworkTaskFactory = new OpenSaveTaskFactory(appManager, pm, swingApp, cyProps);
		final Properties ndexSaveNetworkTaskFactoryProps = new Properties();

		ndexSaveNetworkTaskFactoryProps.setProperty(PREFERRED_MENU, "File.Export");
		ndexSaveNetworkTaskFactoryProps.setProperty(MENU_GRAVITY, "0.0");
		ndexSaveNetworkTaskFactoryProps.setProperty(TITLE, "Network/Collection to NDEx...");
		registerService(bc, ndexSaveNetworkTaskFactory, TaskFactory.class, ndexSaveNetworkTaskFactoryProps);

		
		// TF for NDEx Load
		final OpenBrowseTaskFactory ndexTaskFactory = new OpenBrowseTaskFactory(appManager, icon, pm, swingApp,
				cyProps);
		final Properties ndexTaskFactoryProps = new Properties();
		ndexTaskFactoryProps.setProperty(IN_MENU_BAR, "false");

		registerAllServices(bc, ndexTaskFactory, ndexTaskFactoryProps);

		// Expose CyREST endpoints
		final ErrorBuilder errorBuilder = new ErrorBuilder(ciErrorFactory, ciExceptionFactory, config);
		final NdexClient ndexClient = new NdexClient(errorBuilder);

		// Base
		registerService(bc, new NdexBaseResourceImpl(bc.getBundle().getVersion().toString(), errorBuilder),
				NdexBaseResource.class, new Properties());

		// Status
		registerService(bc, new NdexStatusResourceImpl(pm, errorBuilder), NdexStatusResource.class,
				new Properties());

		// Network IO
		registerService(bc, new NdexNetworkResourceImpl(ndexClient, errorBuilder, appManager, netmgr, tfManager,
				loadNetworkTF, ciExceptionFactory, ciErrorFactory), NdexNetworkResource.class, new Properties());

		// add Handler to remove networks from NetworkManager when deleted
		registerService(bc, new NdexNetworkAboutToBeDestroyedListener(), NetworkAboutToBeDestroyedListener.class,
				new Properties());

		// Remove lock files on start, in case Cytoscape closed unexpectedly
		removeLockFiles();

	}

	private static final void removeLockFiles() {
		for (File f : getLockFiles()) {
			f.delete();
		}
	}

	private final void installWebApp(final String configDir, final BundleContext bc) {

		// This bundle's version
		final Version version = bc.getBundle().getVersion();

		if (!isInstalled(configDir, version.toString())) {
			final File webappDir = new File(configDir, STATIC_CONTENT_DIR);
			webappDir.mkdir();
			extractWebapp(bc.getBundle(), STATIC_CONTENT_DIR, configDir);
			File markerFile = new File(webappDir, INSTALL_MAKER_FILE_NAME + "-" + version.toString() + ".txt");
			try {
				markerFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private final boolean isInstalled(final String configDir, final String bundleVersion) {
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

	private final void copyEntry(final InputStream zis, final String filePath) throws IOException {
		final byte[] buffer = new byte[4096];
		final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
		int read = 0;
		while ((read = zis.read(buffer)) != -1) {
			bos.write(buffer, 0, read);
		}
		bos.close();
	}

	private final boolean checkPort(final int port) {
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

		if (browser != null) {
			browser.getCacheStorage().clearCache();
			if (!browser.isDisposed())
				browser.dispose();
		}

		removeLockFiles();
		super.shutDown();
	}

	private static class CustomPopupHandler implements PopupHandler {
		public PopupContainer handlePopup(PopupParams params) {
			return new PopupContainer() {
				public void insertBrowser(final Browser browser, final Rectangle initialBounds) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							BrowserView browserView = new BrowserView(browser);
							browserView.setPreferredSize(initialBounds.getSize());

							final JFrame frame = new JFrame("Popup");
							frame.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
							frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
							frame.add(browserView, BorderLayout.CENTER);
							frame.pack();
							frame.setLocation(initialBounds.getLocation());
							frame.setVisible(true);
							frame.addWindowListener(new WindowAdapter() {
								@Override
								public void windowClosing(WindowEvent e) {
									browser.dispose();
								}
							});

							browser.addDisposeListener(new DisposeListener<Browser>() {
								public void onDisposed(DisposeEvent<Browser> event) {
									frame.setVisible(false);
								}
							});
						}
					});
				}
			};
		}
	}

}