package org.cytoscape.cyndex2.internal;

import org.cytoscape.cyndex2.internal.singletons.NetworkManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;

public class NdexNetworkAboutToBeDestroyedListener implements NetworkAboutToBeDestroyedListener {

	@Override
	public void handleEvent(NetworkAboutToBeDestroyedEvent arg0) {
		CyNetwork cyNetwork = arg0.getNetwork();
		System.out.println( "Netwwork to be destroyed: " + cyNetwork.getSUID());
		NetworkManager.INSTANCE.deleteCyNetworkEntry(cyNetwork.getSUID());
	}

}
