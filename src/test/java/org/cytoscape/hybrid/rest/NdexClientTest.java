package org.cytoscape.hybrid.rest;

import static org.junit.Assert.*;

import java.util.Map;

import org.cytoscape.hybrid.internal.rest.NdexClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NdexClientTest {

	private NdexClient client;
	
	
	@Before
	public void setUp() throws Exception {
		client = new NdexClient();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testLoad() throws Exception {
		client.load("http://www.ndexbio.org/v2/network/fa2adf68-5363-11e6-b0a6-06603eb7f303", "", "");
	}
	
	@Test
	public void testSummary() throws Exception {
//		final Map<String, ?> summary = client.getSummary(null, 
//				"fa2adf68-5363-11e6-b0a6-06603eb7f303", "", "");
//		System.out.println(summary);
	}

}
