package org.cytoscape.cyndex2.internal;

import static org.cytoscape.work.ServiceProperties.ENABLE_FOR;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.ActionEnableSupport;
import org.cytoscape.ci.CIErrorFactory;
import org.cytoscape.ci.CIExceptionFactory;
import org.cytoscape.ci_bridge_impl.CIProvider;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.cyndex2.internal.rest.NdexClient;
import org.cytoscape.cyndex2.internal.rest.endpoints.NdexBaseResource;
import org.cytoscape.cyndex2.internal.rest.endpoints.NdexNetworkResource;
import org.cytoscape.cyndex2.internal.rest.endpoints.NdexStatusResource;
import org.cytoscape.cyndex2.internal.rest.endpoints.impl.NdexBaseResourceImpl;
import org.cytoscape.cyndex2.internal.rest.endpoints.impl.NdexNetworkResourceImpl;
import org.cytoscape.cyndex2.internal.rest.endpoints.impl.NdexStatusResourceImpl;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorBuilder;
import org.cytoscape.cyndex2.internal.rest.reader.LoadNetworkStreamTaskFactoryImpl;
import org.cytoscape.cyndex2.internal.task.OpenExternalAppTaskFactory;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.cyndex2.server.StaticContentsServer;
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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.BrowserContext;
import com.teamdev.jxbrowser.chromium.JSValue;
import com.teamdev.jxbrowser.chromium.events.ScriptContextAdapter;
import com.teamdev.jxbrowser.chromium.events.ScriptContextEvent;
import com.teamdev.jxbrowser.chromium.internal.ipc.IPCException;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;

public class CyActivator extends AbstractCyActivator {

	// Logger for this activator
	private static final Logger logger = LoggerFactory.getLogger(CyActivator.class);
	public static final String INSTALL_MAKER_FILE_NAME = "ndex-installed";
	Browser browser;
	BrowserView browserView = null;

	private static final String STATIC_CONTENT_DIR = "cyndex-2";

	private JPanel queryPanel;
	private JProgressBar bar;

	private StaticContentsServer httpServer;

	public CyActivator() {
		super();
		initBrowser();
	}

	private void initBrowser() {
		if (browser != null)
			return;
		

		File tempDir = new File(BrowserContext.defaultContext().getDataDir(), "Temp");
		if (!tempDir.exists() || tempDir.listFiles().length == 0) {
			try {
				browser = new Browser();
				browserView = new BrowserView(browser);
				browser.addScriptContextListener(new ScriptContextAdapter() {
					@Override
					public void onScriptContextCreated(ScriptContextEvent event) {
						Browser browser = event.getBrowser();
						JSValue window = browser.executeJavaScriptAndReturnValue("window");
						window.asObject().setProperty("browser", browser);
					}
				});
			} catch (IPCException exception) {
				System.out.println("FAILED TO START Cy-NDEX-2. Please restart Cytoscape");
			}
		}

	}

	public void start(BundleContext bc) {

		// Import dependencies
		final CyApplicationConfiguration config = getService(bc, CyApplicationConfiguration.class);
		final CyApplicationManager appManager = getService(bc, CyApplicationManager.class);
		final CyEventHelper eventHelper = getService(bc, CyEventHelper.class);
		@SuppressWarnings("unchecked")
		final CyProperty<Properties> cyProp = getService(bc, CyProperty.class, "(cyPropertyName=cytoscape3.props)");

		final ExternalAppManager pm = new ExternalAppManager();
		// Create native package (Electron code) locations from properties
		// CIWrapper:truesetURL(cyProp);

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
		TaskFactory loadNetworkTF = new LoadNetworkStreamTaskFactoryImpl(netmgr, networkViewManager, cyProp,
				cyNetworkNaming, vmm, nullNetworkViewFactory, serviceRegistrar);

		// Start static content delivery server
		final File configRoot = config.getConfigurationDirectoryLocation();
		final String staticContentPath = configRoot.getAbsolutePath();

		bar = new JProgressBar();
		bar.setValue(0);
		queryPanel = new JPanel();
		queryPanel.setBorder(new EmptyBorder(0, 10, 0, 10));
		queryPanel.setBackground(new Color(245, 245, 245));

		JLabel label = new JLabel("Loading NDEx App: ");
		queryPanel.setLayout(new BorderLayout());
		queryPanel.add(bar, BorderLayout.CENTER);
		queryPanel.add(label, BorderLayout.WEST);

		// Create web app dir
		installWebApp(staticContentPath, bc);
		File staticPath = new File(staticContentPath, STATIC_CONTENT_DIR);
		this.startHttpServer(bc, staticPath.getAbsolutePath());

		ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource("images/ndex-logo.png"));

		// TF for NDEx Save
		final OpenExternalAppTaskFactory ndexSaveTaskFactory = new OpenExternalAppTaskFactory(
				ExternalAppManager.APP_NAME_SAVE, pm, eventHelper, appManager, browserView, icon, queryPanel);
		final Properties ndexSaveTaskFactoryProps = new Properties();
		ndexSaveTaskFactoryProps.setProperty(ENABLE_FOR, ActionEnableSupport.ENABLE_FOR_NETWORK);
		ndexSaveTaskFactoryProps.setProperty(PREFERRED_MENU, "File.Export");
		ndexSaveTaskFactoryProps.setProperty(MENU_GRAVITY, "0.0");
		ndexSaveTaskFactoryProps.setProperty(TITLE, "Network Collection to NDEx...");
		registerAllServices(bc, ndexSaveTaskFactory, ndexSaveTaskFactoryProps);

		// TF for NDEx Load
		final OpenExternalAppTaskFactory ndexTaskFactory = new OpenExternalAppTaskFactory(
				ExternalAppManager.APP_NAME_LOAD, pm, eventHelper, appManager, browserView, icon, queryPanel);
		final Properties ndexTaskFactoryProps = new Properties();
		ndexTaskFactoryProps.setProperty(IN_MENU_BAR, "false");

		registerAllServices(bc, ndexTaskFactory, ndexTaskFactoryProps);

		// Expose CyREST endpoints
		final ErrorBuilder errorBuilder = new ErrorBuilder(ciErrorFactory, ciExceptionFactory, config);
		final NdexClient ndexClient = new NdexClient(errorBuilder);

		// Status endpoint

		// Base
		registerService(bc, new NdexBaseResourceImpl(bc.getBundle().getVersion().toString(), errorBuilder),
				NdexBaseResource.class, new Properties());

		// Status
		registerService(bc, new NdexStatusResourceImpl(pm, errorBuilder, appManager), NdexStatusResource.class,
				new Properties());

		// Network IO
		registerService(bc, new NdexNetworkResourceImpl(ndexClient, errorBuilder, appManager, netmgr, tfManager,
				loadNetworkTF, ciExceptionFactory, ciErrorFactory), NdexNetworkResource.class, new Properties());

	}

	private final void installWebApp(final String configDir, final BundleContext bc) {

		// This bundle's version
		final Version version = bc.getBundle().getVersion();

		if (!isInstalled(configDir, version.toString())) {
			final File webappDir = new File(configDir, STATIC_CONTENT_DIR);
			webappDir.mkdir();
			bar.setValue(10);
			extractWebapp(bc.getBundle(), STATIC_CONTENT_DIR, configDir);
			File markerFile = new File(configDir, INSTALL_MAKER_FILE_NAME + "-" + version.toString() + ".txt");
			try {
				markerFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		bar.setValue(100);
		queryPanel.removeAll();
		queryPanel.repaint();
	}

	private final boolean isInstalled(final String configDir, final String bundleVersion) {
		// This is the indicator of installation.
		final String markerFileName = INSTALL_MAKER_FILE_NAME + "-" + bundleVersion + ".txt";
		final File markerFile = new File(configDir, markerFileName);

		if (markerFile.exists()) {
			// Exact match required. Otherwise, simply override the existing
			// contents.
			return true;
		}

		return false;
	}

	private final void extractWebapp(final Bundle bundle, final String path, String targetDir) {
		Enumeration<String> ress = bundle.getEntryPaths(path);
		bar.setValue(50);
		while (ress.hasMoreElements()) {
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
			bar.setIndeterminate(true);
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
			return;
		}

		httpServer = new StaticContentsServer(path);
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			try {
				httpServer.startServer();
			} catch (Exception e) {
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
			e.printStackTrace();
		}
		if (browser != null)
			browser.dispose();

	}

}