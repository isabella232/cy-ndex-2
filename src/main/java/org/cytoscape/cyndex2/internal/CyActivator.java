package org.cytoscape.cyndex2.internal;

import static org.cytoscape.work.ServiceProperties.*;

import java.awt.Dialog.ModalityType;
import java.io.File;
import java.util.Dictionary;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JDialog;

import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.cyndex2.internal.rest.NdexClient;
import org.cytoscape.cyndex2.internal.rest.endpoints.NdexBaseResource;
import org.cytoscape.cyndex2.internal.rest.endpoints.NdexNetworkResource;
import org.cytoscape.cyndex2.internal.rest.endpoints.NdexStatusResource;
import org.cytoscape.cyndex2.internal.rest.endpoints.impl.NdexBaseResourceImpl;
import org.cytoscape.cyndex2.internal.rest.endpoints.impl.NdexNetworkResourceImpl;
import org.cytoscape.cyndex2.internal.rest.endpoints.impl.NdexStatusResourceImpl;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorBuilder;
import org.cytoscape.cyndex2.internal.singletons.CyObjectManager;
import org.cytoscape.cyndex2.internal.task.OpenBrowseTaskFactory;
import org.cytoscape.cyndex2.internal.task.OpenSaveTaskFactory;
import org.cytoscape.cyndex2.internal.ui.ImportNetworkFromNDExTaskFactory;
import org.cytoscape.cyndex2.internal.ui.SaveNetworkToNDExTaskFactory;
import org.cytoscape.cyndex2.internal.util.BrowserManager;
import org.cytoscape.cyndex2.internal.util.CIServiceManager;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.cyndex2.internal.util.StringResources;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.write.CyNetworkViewWriterFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNetworkTableManager;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.task.NetworkViewCollectionTaskFactory;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CyActivator extends AbstractCyActivator {

	// Logger for this activator
	private static final Logger logger = LoggerFactory.getLogger(CyActivator.class);
	public static final String WEB_APP_VERSION = "0.1.2";

	private static CyProperty<Properties> cyProps;

	private static String appVersion;
	private static String cytoscapeVersion;
	private static String appName;
	private static boolean hasCyNDEx1;

	private static JDialog dialog;
	private CIServiceManager ciServiceManager;
	private static CySwingApplication swingApp;
	public static TaskManager<?, ?> taskManager;

	public CyActivator() {
		super();
		
		hasCyNDEx1 = false;
	}
	
	public static String getProperty(String prop) {
		return cyProps.getProperties().getProperty(prop);
	}

	public static JDialog getDialog() {
		if (dialog == null) {
			dialog = new JDialog(swingApp.getJFrame(), "CyNDEx2 Browser", ModalityType.APPLICATION_MODAL);
			// ensure modality type
			dialog.getModalityType();
		}
		return dialog;
	}

	public static String getCyRESTPort() {
		String port = cyProps.getProperties().getProperty("rest.port");
		if (port == null) {
			return "1234";
		}
		return port;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void start(BundleContext bc) throws InvalidSyntaxException {

		for (Bundle b : bc.getBundles()) {
			// System.out.println(b.getSymbolicName());
			if (b.getSymbolicName().equals("org.cytoscape.api-bundle")) {
				cytoscapeVersion = b.getVersion().toString();
				// break;
			} else if (b.getSymbolicName().equals("org.cytoscape.ndex.cyNDEx")) {
				/*
				 * Version v = b.getVersion(); System.out.println(v); int st = b.getState();
				 * System.out.println(st);
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
		taskManager = getService(bc, TaskManager.class);

		swingApp = getService(bc, CySwingApplication.class);
		final CySwingAppAdapter appAdapter = getService(bc, CySwingAppAdapter.class);
		final CyNetworkTableManager networkTableManager = getService(bc, CyNetworkTableManager.class);

		// Register these with the CyObjectManager singleton.
		CyObjectManager manager = CyObjectManager.INSTANCE;
		File configDir = config.getAppConfigurationDirectoryLocation(CyActivator.class);
		configDir.mkdirs();
		manager.setConfigDir(configDir);
		manager.setCySwingAppAdapter(appAdapter);
		manager.setNetworkTableManager(networkTableManager);

		// For loading network
		final CxTaskFactoryManager tfManager = new CxTaskFactoryManager();
		registerServiceListener(bc, tfManager, "addReaderFactory", "removeReaderFactory", InputStreamTaskFactory.class);
		registerServiceListener(bc, tfManager, "addWriterFactory", "removeWriterFactory",
				CyNetworkViewWriterFactory.class);

		ciServiceManager = new CIServiceManager(bc);

		// Create subdirectories in config dir for jxbrowser
		final CyNetworkManager netmgr = getService(bc, CyNetworkManager.class);
		File jxBrowserDir = new File(config.getConfigurationDirectoryLocation(), "jxbrowser");
		jxBrowserDir.mkdir();
		BrowserManager.setDataDirectory(new File(jxBrowserDir, "data"));
		
		// get QueryPanel icon
		ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource("images/ndex-logo.png"));

		// TF for NDEx Save Network
		final OpenSaveTaskFactory ndexSaveNetworkTaskFactory = new OpenSaveTaskFactory(ExternalAppManager.SAVE_NETWORK,
				appManager);
		final Properties ndexSaveNetworkTaskFactoryProps = new Properties();

		ndexSaveNetworkTaskFactoryProps.setProperty(PREFERRED_MENU, "File.Export");
		ndexSaveNetworkTaskFactoryProps.setProperty(MENU_GRAVITY, "0.0");
		ndexSaveNetworkTaskFactoryProps.setProperty(TITLE, "Network to NDEx...");
		registerService(bc, ndexSaveNetworkTaskFactory, TaskFactory.class, ndexSaveNetworkTaskFactoryProps);

		// TF for NDEx Save Collection
		final OpenSaveTaskFactory ndexSaveCollectionTaskFactory = new OpenSaveTaskFactory(
				ExternalAppManager.SAVE_COLLECTION, appManager);
		final Properties ndexSaveCollectionTaskFactoryProps = new Properties();

		ndexSaveCollectionTaskFactoryProps.setProperty(PREFERRED_MENU, "File.Export");
		ndexSaveCollectionTaskFactoryProps.setProperty(MENU_GRAVITY, "0.1");
		ndexSaveCollectionTaskFactoryProps.setProperty(TITLE, "Collection to NDEx...");
		registerService(bc, ndexSaveCollectionTaskFactory, TaskFactory.class, ndexSaveCollectionTaskFactoryProps);
		
		ImportNetworkFromNDExTaskFactory importFromNDExTaskFactory = new ImportNetworkFromNDExTaskFactory(ExternalAppManager.APP_NAME_LOAD);
		Properties importProps = new Properties();
		importProps.setProperty(IN_TOOL_BAR, "true");
		importProps.setProperty(TOOL_BAR_GRAVITY, "0.0f");
		importProps.setProperty(TOOLTIP, StringResources.NDEX_OPEN);
		String loadIconUrl = getClass().getResource("/images/open_from_ndex_36x36.png").toString();
		importProps.setProperty(LARGE_ICON_URL, loadIconUrl);
		importProps.setProperty(SMALL_ICON_URL, loadIconUrl);
		registerService(bc, importFromNDExTaskFactory, TaskFactory.class, importProps);

		
		SaveNetworkToNDExTaskFactory saveToNDExTaskFactory = new SaveNetworkToNDExTaskFactory(appManager, ExternalAppManager.APP_NAME_SAVE);
		Properties props = new Properties();
		props.setProperty(IN_TOOL_BAR, "true");
		props.setProperty(INSERT_TOOLBAR_SEPARATOR_AFTER, "true");
		props.setProperty(TOOLTIP, StringResources.NDEX_SAVE);
		props.setProperty(TOOL_BAR_GRAVITY, "0.1f");
		String saveIconUrl = getClass().getResource("/images/save_to_ndex_36x36.png").toString();
		props.setProperty(LARGE_ICON_URL, saveIconUrl);
		props.setProperty(SMALL_ICON_URL, saveIconUrl);
		registerService(bc, saveToNDExTaskFactory, TaskFactory.class, props);

		// TF for NDEx Load
		final OpenBrowseTaskFactory ndexTaskFactory = new OpenBrowseTaskFactory(icon);
		final Properties ndexTaskFactoryProps = new Properties();
		// ndexTaskFactoryProps.setProperty(IN_MENU_BAR, "false");
		ndexTaskFactoryProps.setProperty(PREFERRED_MENU, "File.Import");
		ndexTaskFactoryProps.setProperty(MENU_GRAVITY, "0.0");
		ndexTaskFactoryProps.setProperty(TITLE, "Network from NDEx...");
		registerAllServices(bc, ndexTaskFactory, ndexTaskFactoryProps);

		// Expose CyREST endpoints
		final ErrorBuilder errorBuilder = new ErrorBuilder(ciServiceManager, config);
		final NdexClient ndexClient = new NdexClient(errorBuilder);

		// Base
		registerService(bc,
				new NdexBaseResourceImpl(bc.getBundle().getVersion().toString(), errorBuilder, ciServiceManager),
				NdexBaseResource.class, new Properties());

		// Status
		registerService(bc, new NdexStatusResourceImpl(errorBuilder, ciServiceManager), NdexStatusResource.class,
				new Properties());

		// Network IO
		registerService(bc, new NdexNetworkResourceImpl(ndexClient, errorBuilder, appManager, netmgr, ciServiceManager),
				NdexNetworkResource.class, new Properties());

		// add Handler to remove networks from NetworkManager when deleted
		registerService(bc, new NdexNetworkAboutToBeDestroyedListener(), NetworkAboutToBeDestroyedListener.class,
				new Properties());

		OpenSaveTaskFactory saveNetworkToNDExContextMenuTaskFactory = new OpenSaveTaskFactory(
				ExternalAppManager.SAVE_NETWORK, appManager);
		Properties saveNetworkToNDExContextMenuProps = new Properties();
		saveNetworkToNDExContextMenuProps.setProperty(ID, "exportToNDEx");
		saveNetworkToNDExContextMenuProps.setProperty(TITLE, "Export Network to NDEx...");

		saveNetworkToNDExContextMenuProps.setProperty(IN_NETWORK_PANEL_CONTEXT_MENU, "true");
		saveNetworkToNDExContextMenuProps.setProperty(INSERT_SEPARATOR_BEFORE, "true");
		saveNetworkToNDExContextMenuProps.setProperty(ENABLE_FOR, "network");

		registerService(bc, saveNetworkToNDExContextMenuTaskFactory, NetworkViewCollectionTaskFactory.class,
				saveNetworkToNDExContextMenuProps);
		// registerService(bc, saveNetworkToNDExContextMenuTaskFactory,
		// TestTaskFactory.class, saveNetworkToNDExContextMenuProps);
	}

	@Override
	public void shutDown() {
		logger.info("Shutting down CyNDEx-2...");

		BrowserManager.clearCache();
		if (ciServiceManager != null) {
			ciServiceManager.close();
		}
		BrowserManager.shutdown();
		
		super.shutDown();
	}

	public static String getCyVersion() {
		return cytoscapeVersion;
	}

	public static String getAppName() {
		return appName;
	}

	public static boolean hasCyNDEx1() {
		return hasCyNDEx1;
	}

	public static void setHasCyNDEX1(boolean hasCyNDEx1) {
		CyActivator.hasCyNDEx1 = hasCyNDEx1;
	}

	public static String getAppVersion() {
		return appVersion;
	}

}