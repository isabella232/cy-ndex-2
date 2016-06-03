package org.cytoscape.hybrid.events;

import org.cytoscape.event.CyListener;

public interface WebSocketEventListener extends CyListener {
	
	void handleEvent(final WebSocketEvent e);

}
