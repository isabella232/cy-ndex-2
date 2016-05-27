package org.cytoscape.fx.internal;

import java.awt.Container;
import java.awt.Dimension;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JSplitPane;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.fx.internal.task.HeadlessTaskMonitor;
import org.cytoscape.fx.internal.ui.NdexMainPanel;
import org.cytoscape.fx.internal.ws.JClient;
import org.cytoscape.fx.internal.ws.WSServer;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.group.CyGroupManager;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.io.write.CyNetworkViewWriterFactory;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.task.create.NewNetworkSelectedNodesAndEdgesTaskFactory;
import org.cytoscape.task.create.NewSessionTaskFactory;
import org.cytoscape.task.read.LoadNetworkURLTaskFactory;
import org.cytoscape.task.read.OpenSessionTaskFactory;
import org.cytoscape.task.select.SelectFirstNeighborsTaskFactory;
import org.cytoscape.task.write.ExportNetworkViewTaskFactory;
import org.cytoscape.task.write.SaveSessionAsTaskFactory;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.RenderingEngineManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.TaskMonitor;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CyActivator extends AbstractCyActivator {

	private static final Logger logger = LoggerFactory.getLogger(CyActivator.class);

	public CyActivator() {
		super();
	}

	public void start(BundleContext bc) {

		final CyProperty<Properties> cyPropertyServiceRef = getService(bc, CyProperty.class,
				"(cyPropertyName=cytoscape3.props)");
		final TaskMonitor headlessTaskMonitor = new HeadlessTaskMonitor();
		final CyNetworkFactory netFact = getService(bc, CyNetworkFactory.class);
		final CyNetworkViewFactory netViewFact = getService(bc, CyNetworkViewFactory.class);
		final CyNetworkManager netMan = getService(bc, CyNetworkManager.class);
		final CyRootNetworkManager cyRootNetworkManager = getService(bc, CyRootNetworkManager.class);
		final CyNetworkViewManager netViewMan = getService(bc, CyNetworkViewManager.class);
		final VisualMappingManager visMan = getService(bc, VisualMappingManager.class);
		final CyApplicationManager applicationManager = getService(bc, CyApplicationManager.class);
		final CyApplicationConfiguration appConfig = getService(bc, CyApplicationConfiguration.class);
		final CyLayoutAlgorithmManager layoutManager = getService(bc, CyLayoutAlgorithmManager.class);
		final VisualStyleFactory vsFactory = getService(bc, VisualStyleFactory.class);
		final CyGroupFactory groupFactory = getService(bc, CyGroupFactory.class);
		final CyGroupManager groupManager = getService(bc, CyGroupManager.class);
		final CyTableManager tableManager = getService(bc, CyTableManager.class);
		final CyTableFactory tableFactory = getService(bc, CyTableFactory.class);
		final StreamUtil streamUtil = getService(bc, StreamUtil.class);
		final CySessionManager sessionManager = getService(bc, CySessionManager.class);
		final SaveSessionAsTaskFactory saveSessionAsTaskFactory = getService(bc, SaveSessionAsTaskFactory.class);
		final OpenSessionTaskFactory openSessionTaskFactory = getService(bc, OpenSessionTaskFactory.class);
		final NewSessionTaskFactory newSessionTaskFactory = getService(bc, NewSessionTaskFactory.class);
		final CySwingApplication desktop = getService(bc, CySwingApplication.class);
		final ExportNetworkViewTaskFactory exportNetworkViewTaskFactory = getService(bc,
				ExportNetworkViewTaskFactory.class);

		final OpenBrowser browser = getService(bc, OpenBrowser.class);

		// Task factories
		final NewNetworkSelectedNodesAndEdgesTaskFactory networkSelectedNodesAndEdgesTaskFactory = getService(bc,
				NewNetworkSelectedNodesAndEdgesTaskFactory.class);
		final CyNetworkViewWriterFactory cytoscapeJsWriterFactory = getService(bc, CyNetworkViewWriterFactory.class,
				"(id=cytoscapejsNetworkWriterFactory)");
		final InputStreamTaskFactory cytoscapeJsReaderFactory = getService(bc, InputStreamTaskFactory.class,
				"(id=cytoscapejsNetworkReaderFactory)");

		final LoadNetworkURLTaskFactory loadNetworkURLTaskFactory = getService(bc, LoadNetworkURLTaskFactory.class);
		final SelectFirstNeighborsTaskFactory selectFirstNeighborsTaskFactory = getService(bc,
				SelectFirstNeighborsTaskFactory.class);

		final NetworkTaskFactory fitContent = getService(bc, NetworkTaskFactory.class, "(title=Fit Content)");
		final NetworkTaskFactory edgeBundler = getService(bc, NetworkTaskFactory.class, "(title=All Nodes and Edges)");
		final NetworkTaskFactory showDetailsTaskFactory = getService(bc, NetworkTaskFactory.class,
				"(title=Show/Hide Graphics Details)");

		final RenderingEngineManager renderingEngineManager = getService(bc, RenderingEngineManager.class);

		final CySwingApplication cySwingApplicationServiceRef = getService(bc, CySwingApplication.class);
		final CyServiceRegistrar registrar = getService(bc, CyServiceRegistrar.class);

		// final ShowPanelTaskFactory showMessageTaskFactory = new
		// ShowPanelTaskFactory(registrar,
		// cySwingApplicationServiceRef, appConfig, browser);
		// Properties showMessageTaskFactoryProps = new Properties();
		// showMessageTaskFactoryProps.setProperty(ID,
		// "showMessageTaskFactory");
		// showMessageTaskFactoryProps.setProperty(PREFERRED_MENU, "Tools");
		// showMessageTaskFactoryProps.setProperty(TITLE, "Open NDEx Valet...");
		// showMessageTaskFactoryProps.setProperty(MENU_GRAVITY, "1.0");
		// registerService(bc, showMessageTaskFactory, TaskFactory.class,
		// showMessageTaskFactoryProps);

		// PanelStatusManager panelStatusManager = new
		// PanelStatusManager(showMessageTaskFactory,
		// cySwingApplicationServiceRef, registrar);
		// registerService(bc, panelStatusManager,
		// CytoPanelStateChangedListener.class, new Properties());

		// This is a singleton
		final NdexMainPanel panel = new NdexMainPanel(cySwingApplicationServiceRef);
		registerAllServices(bc, panel, new Properties());
		CytoPanel parent = cySwingApplicationServiceRef.getCytoPanel(CytoPanelName.SOUTH_WEST);
		panel.setPreferredSize(new Dimension(600, 1000));
		panel.setMinimumSize(new Dimension(400, 600));
		panel.setSize(new Dimension(600, 1000));

		parent.setState(CytoPanelState.DOCK);
		parent.setSelectedIndex(parent.indexOfComponent(panel));
		Container splitpane = parent.getThisComponent().getParent();
		System.out.println(splitpane);
		((JSplitPane) splitpane).setDividerLocation(0.8);

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

		System.out.println("********** Server is OK.  Starting client...");
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		

	}

	@Override
	public void shutDown() {
		logger.info("Shutting down NDEx Valet...");
	}
}