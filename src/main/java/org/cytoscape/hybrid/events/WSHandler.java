package org.cytoscape.hybrid.events;

import org.eclipse.jetty.websocket.api.Session;

public interface WSHandler {
	
	public void handleMessage(final InterAppMessage msg, final Session session);
	
	// Return compatible message type
	public String getType();

}
