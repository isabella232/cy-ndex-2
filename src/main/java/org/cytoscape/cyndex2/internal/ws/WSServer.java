package org.cytoscape.cyndex2.internal.ws;

import javax.websocket.server.ServerContainer;

import org.cytoscape.application.events.CyShutdownEvent;
import org.cytoscape.application.events.CyShutdownListener;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WSServer implements CyShutdownListener {

	private static final Logger logger = LoggerFactory.getLogger(WSServer.class);

	private static final Integer DEF_PORT = 8025;
	private static final String DEF_IP = "0.0.0.0";
	private static final String DEF_ENDPOINT = "/ws";

	private final String url;
	private final Server server;

	public WSServer() {
		this.server = new Server();
		this.url = "ws://" + DEF_IP + ":" + DEF_PORT + DEF_ENDPOINT;
	}

	public void start() throws Exception {
		logger.info("Initializing WS Server on port: " + DEF_PORT);

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
		server.start();
		logger.info("WS Server listening on " + url);
		
		server.join();
	}
	
	
	public void stop() {
		try {
			server.stop();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Could not stop WS server.", e);
		}
	}
	
	public Server getServer() {
		return this.server;
	}

	@Override
	public void handleEvent(CyShutdownEvent evt) {
		logger.info("CyNDEx-2 App");
		
//		try {
//			server.stop();
//		} catch (Exception e) {
//			e.printStackTrace();
//			logger.error("Could not stop WS server.", e);
//		}
	}
}