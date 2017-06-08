package org.cytoscape.hybrid.internal.rest.reader;

import java.util.Properties;

import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;


/**
 * Custom Task Factory for loading 
 */
public class LoadNetworkStreamTaskFactoryImpl implements TaskFactory, CxReaderFactory {

	private CyNetworkManager netmgr;
	
	private final CyNetworkViewManager networkViewManager;
	private Properties props;

	private CyNetworkNaming cyNetworkNaming;
	
	private final VisualMappingManager vmm;
	private final CyNetworkViewFactory nullNetworkViewFactory;
	private final CyServiceRegistrar serviceRegistrar;

	public LoadNetworkStreamTaskFactoryImpl(
			final CyNetworkManager netmgr,
			final CyNetworkViewManager networkViewManager,
			final CyProperty<Properties> cyProps,
			final CyNetworkNaming cyNetworkNaming,
			final VisualMappingManager vmm,
			final CyNetworkViewFactory nullNetworkViewFactory,
			final CyServiceRegistrar serviceRegistrar
	) {
		this.netmgr = netmgr;
		this.networkViewManager = networkViewManager;
		this.props = cyProps.getProperties();
		this.cyNetworkNaming = cyNetworkNaming;
		this.vmm = vmm;
		this.nullNetworkViewFactory = nullNetworkViewFactory;
		this.serviceRegistrar = serviceRegistrar;
	}

	
	@Override
	public TaskIterator createTaskIterator(final String networkName, final CyNetworkReader reader) {
		return loadCyNetworks(networkName, reader);
	}

	public TaskIterator loadCyNetworks(String name, final CyNetworkReader reader) {
		return new TaskIterator(2, new LoadNetworkTask(netmgr, reader, name, networkViewManager, props,
				cyNetworkNaming, vmm, nullNetworkViewFactory, serviceRegistrar));
	}

	@Override
	public TaskIterator createTaskIterator() {
		return null;
	}

	@Override
	public boolean isReady() {
		return true;
	}
}
