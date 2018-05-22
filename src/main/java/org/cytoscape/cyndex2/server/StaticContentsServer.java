package org.cytoscape.cyndex2.server;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class StaticContentsServer {
	
	private static final Logger logger = LoggerFactory.getLogger(StaticContentsServer.class);
	private static final int PORT = 2222;
	
	private Server server;
	
	private final String path;
	
	public StaticContentsServer(final String basePath) {
		this.path = basePath;
	}
	
	
	public void startServer() throws Exception {
		this.server = new Server(PORT);
		final ResourceHandler resource_handler = new ResourceHandler();
		resource_handler.setDirectoriesListed(true);
		resource_handler.setWelcomeFiles(new String[] { "index.html" });
		resource_handler.setResourceBase(this.path);

		// Add the ResourceHandler to the server.
		GzipHandler gzip = new GzipHandler();
		server.setHandler(gzip);
		HandlerList handlers = new HandlerList();
		handlers.setHandlers(new Handler[] { resource_handler, new DefaultHandler() });
		gzip.setHandler(handlers);

		try {
			server.start();
		} catch(Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Faild to start static content server.", ex);
		}
		logger.info("Preview server is listening on port " + PORT);
	}
	
	public void stopServer() throws Exception {
		server.stop();
	}
}