package org.cytoscape.fx.internal.ws;

import java.net.URI;

import org.cytoscape.application.swing.CySwingApplication;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

public class WSClient {

	private JClient socket;
	private WebSocketClient client;

	private final CySwingApplication app;
	private final ProcessManager pm;
	
	public WSClient(final CySwingApplication app, ProcessManager pm) {
		this.app = app;
		this.pm = pm;
	}
	
	public void start(String dest) throws Exception {

		client = new WebSocketClient();
		System.out.println("Creating client*************");
		
		socket = new JClient(app, pm);
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
