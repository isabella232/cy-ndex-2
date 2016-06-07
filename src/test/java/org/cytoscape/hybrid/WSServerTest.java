package org.cytoscape.hybrid;

import static org.junit.Assert.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.cytoscape.hybrid.internal.ws.WSServer;
import org.eclipse.jetty.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class WSServerTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testStartServer() throws Exception {

		final WSServer server = new WSServer();
		startServerThread(server);

		System.out.println("Running**************");

		Thread.sleep(1000);
		testServer(server);
	}
	
	private void testServer(final WSServer server) {
		final Server serv = server.getServer();
		
		assertTrue(serv.isRunning());
		System.out.println(serv.dump());
		System.out.println("URI: " + serv.getURI());
		assertEquals("http://0.0.0.0:8025/ws", serv.getURI().toString());
		
		
	}


	private void startServerThread(final WSServer server) {
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			try {
				server.start();
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		});
	}

}
