package org.cytoscape.hybrid.internal.ws;

import java.net.URI;
import java.util.Properties;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.hybrid.internal.login.LoginManager;
import org.cytoscape.property.CyProperty;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

public class WSClient {

	private final ClientSocket socket;
	private WebSocketClient client;


	public WSClient(final CySwingApplication app, final ExternalAppManager pm, CyEventHelper eventHelper,
			final LoginManager loginManager, final CyProperty<Properties> props) {
		socket = new ClientSocket(app, pm, loginManager, props);
		client = new WebSocketClient();
		client.getPolicy().setIdleTimeout(1000000000);
		client.setMaxIdleTimeout(1000000000);
	}
	

	public final void start(String dest) throws Exception {
		
		final URI echoUri = new URI(dest);
		final ClientUpgradeRequest request = new ClientUpgradeRequest();
		
		if(client.isStarted() == false) {
			client.start();
			client.connect(socket, echoUri, request);
			socket.getLatch().await();
		}
	}


	public ClientSocket getSocket() {
		return socket;
	}

	public final void close() {
		try {
			if(client.isStopped() == false && client.isStopping() == false) {
				client.stop();
			} else {
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean isStopped() {
		return client.isStopped();
	}

}
