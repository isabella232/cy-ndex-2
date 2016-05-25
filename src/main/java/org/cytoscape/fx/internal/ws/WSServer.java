package org.cytoscape.fx.internal.ws;

import javax.servlet.ServletException;
import javax.websocket.server.ServerContainer;

import org.cytoscape.application.events.CyShutdownEvent;
import org.cytoscape.application.events.CyShutdownListener;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

public class WSServer implements CyShutdownListener {

	private static final Integer DEF_PORT = 8025;
	private static final String DEF_IP = "0.0.0.0";
	
	private static final String DEF_ENDPOINT = "/ws";
	
	private final Server server;
	
	
	public WSServer() {
		this.server = new Server();
	}

	public void start() throws Exception {
		System.out.println("Initializing WS Server on port: " + DEF_PORT);
		
		final ServerConnector connector = new ServerConnector(server);
		connector.setPort(DEF_PORT);
		connector.setHost(DEF_IP);
		server.addConnector(connector);

		final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath(DEF_ENDPOINT);
		server.setHandler(context);

		final ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);
		
		// Add end points
		wscontainer.addEndpoint(EchoEndpoint.class);

		System.out.println("Starting WS...");
		server.start();
		
		System.out.println("Listening on " + "ws://" + DEF_IP + ":" + DEF_PORT + DEF_ENDPOINT);
		server.join();
	}

	@Override
	public void handleEvent(CyShutdownEvent evt) {
		System.out.println("Terminating WS Server: " + DEF_PORT);
		try {
			server.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
