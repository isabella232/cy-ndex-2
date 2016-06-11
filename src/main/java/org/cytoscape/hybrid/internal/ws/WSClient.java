package org.cytoscape.hybrid.internal.ws;

import java.net.URI;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.event.CyEventHelper;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

public class WSClient {

	private ClientSocket socket;
	private WebSocketClient client;

	private final CySwingApplication app;
	private final ExternalAppManager pm;
	private final CyEventHelper eventHelper;
	
	
	public WSClient(final CySwingApplication app, final ExternalAppManager pm, CyEventHelper eventHelper) {
		this.app = app;
		this.pm = pm;
		this.eventHelper = eventHelper;
	}
	
	public void start(String dest) throws Exception {
		client = new WebSocketClient();
		client.getPolicy().setIdleTimeout(1000000000);
		socket = new ClientSocket(app, pm, eventHelper);
		client.start();
		URI echoUri = new URI(dest);
		ClientUpgradeRequest request = new ClientUpgradeRequest();
		client.connect(socket, echoUri, request);
		socket.getLatch().await();
	}
	
	public Boolean isStopped() {
		return client.isStopped(); 
	}
	
	public ClientSocket getSocket() {
		return socket;
	}

}
