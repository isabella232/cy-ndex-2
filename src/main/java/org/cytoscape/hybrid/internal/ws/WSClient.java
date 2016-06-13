package org.cytoscape.hybrid.internal.ws;

import java.net.URI;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
	private String dest;
	
	
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
		this.dest = dest;
		
		ClientUpgradeRequest request = new ClientUpgradeRequest();
		client.connect(socket, echoUri, request);
		client.setMaxIdleTimeout(1000000000);
		socket.getLatch().await(10000, TimeUnit.SECONDS);
	}
	
	public Boolean isStopped() {
		return client.isStopped(); 
	}
	
	public ClientSocket getSocket() {
		if(!socket.isOpen()) {
			try {
				start(dest);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return socket;
	}

}
