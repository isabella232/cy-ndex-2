package org.cytoscape.fx.internal.ws;

import java.net.URI;

import org.cytoscape.application.swing.CySwingApplication;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

public class WSClient {

	private JClient socket;
	private WebSocketClient client;

	private final CySwingApplication app;
	
	public WSClient(final CySwingApplication app) {
		this.app = app;
	}
	
	public void start(String dest) throws Exception {

		client = new WebSocketClient();
		System.out.println("Creating client*************");
		
		socket = new JClient(app);
		client.start();
		System.out.println("Client is OK*************");
		URI echoUri = new URI(dest);
		ClientUpgradeRequest request = new ClientUpgradeRequest();
		client.connect(socket, echoUri, request);
		socket.getLatch().await();
	}
	
	public Boolean isStopped() {
		return client.isStopped(); 
	}
	
	public JClient getSocket() {
		return socket;
	}

}
