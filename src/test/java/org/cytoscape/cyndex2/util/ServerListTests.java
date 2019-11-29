package org.cytoscape.cyndex2.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import org.cytoscape.cyndex2.internal.util.Server;
import org.cytoscape.cyndex2.internal.util.ServerKey;
import org.cytoscape.cyndex2.internal.util.ServerList;
import org.junit.Test;

public class ServerListTests {
	
	@Test
	public void serverListReadAndWrite() {
		TestUtil.init(true);
		Server server = new Server();
		server.setUrl("http://mockserver");
		server.setUsername("mockuser");
		server.setPassword("mockpassword");
		
		ServerList serverList = new ServerList();
		assertEquals(0, serverList.getSize());
		
			try {
				serverList.add(server);
			} catch (Exception e) {
				e.printStackTrace();
				fail();
			}
		
			File file = TestUtil.getResource("testReadWrite", "addedServers.json");
			
			serverList.writeServerList(file);
			
			ServerList retrievedList = ServerList.readServerList(file);
			
			assertEquals(1, retrievedList.getSize());
			
			Server retrievedServer = retrievedList.getElementAt(0);
			
			assertEquals("http://mockserver", retrievedServer.getUrl());
			assertEquals("mockuser", retrievedServer.getUsername());
			assertEquals("mockpassword", retrievedServer.getPassword());
	}
	
	@Test
	public void serverListAdd() {
		Server server = new Server();
		server.setUrl("http://mockserver");
		server.setUsername("mockuser");
		server.setPassword("mockpassword");
		
		ServerList serverList = new ServerList();
		assertEquals(0, serverList.getSize());
		
		try {
			serverList.add(server);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		assertEquals(1, serverList.getSize());
		
		Server retrievedServer = serverList.getServer(new ServerKey("mockuser","http://mockserver"));
	
		assertEquals("http://mockserver", retrievedServer.getUrl());
		assertEquals("mockuser", retrievedServer.getUsername());
		assertEquals("mockpassword", retrievedServer.getPassword());
		
		serverList.delete(retrievedServer);
		
		assertEquals(0, serverList.getSize());
	}
}



