package org.cytoscape.cyndex2.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.cytoscape.cyndex2.internal.util.Server;
import org.junit.Test;

public class ServerTests {
	@Test
	public void serverFields() {
		Server server = new Server();
		server.setUrl("aaaa");
		server.setUserId(new UUID(1l,2l));
		server.setUsername("cccc");
		server.setPassword("dddd");
		
		assertEquals("aaaa", server.getUrl());
		assertEquals(new UUID(1l, 2l), server.getUserId());
		assertEquals("cccc", server.getUsername());
		assertEquals("dddd", server.getPassword());

	}
	
	@Test
	public void checkEmptyServerEquality() {
		Server a = new Server();
		Server b = new Server();
		
		assertTrue(a.equals(b));
		assertTrue(b.equals(a));
	}
	
	@Test 
	public void checkEmptyUsernameEquality() {
		Server a = new Server();
		a.setUrl("aaaa");
		
		Server b = new Server();
		b.setUrl("bbbb");
		
		Server c = new Server();
		c.setUrl("aaaa");
		
		assertFalse(a.equals(b));
		assertTrue(a.equals(c));
		
	}
	
	@Test 
	public void checkNonNullUsernameEquality() {
		Server a = new Server();
		a.setUrl("aaaa");
		a.setUsername("auser");
		
		Server b = new Server();
		b.setUrl("bbbb");
		b.setUsername("auser");
		
		Server c = new Server();
		c.setUrl("aaaa");
		c.setUsername("auser");
		
		Server d = new Server();
		d.setUrl("aaaa");
		d.setUsername("xuser");
		
		assertFalse(a.equals(b));
		assertFalse(a.equals(d));
		
		assertTrue(a.equals(c));
	}
	
	
	
}



