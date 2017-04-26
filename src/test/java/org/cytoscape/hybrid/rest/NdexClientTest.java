package org.cytoscape.hybrid.rest;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.Map;

import org.cytoscape.hybrid.internal.rest.NdexClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.cglib.transform.impl.InterceptFieldFilter;

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
		final Map<String, ?> summary = client.getSummary(null, 
				"fa2adf68-5363-11e6-b0a6-06603eb7f303");
		System.out.println(summary);
		
	}
	
	@Test(expected=RuntimeException.class)
	public void testSummaryInvalid() throws Exception {
		
		final Map<String, ?> summary2 = client.getSummary(null, 
				"invalid-uuid");
	}
	
	@Test(expected=RuntimeException.class)
	public void testSummaryInvalid2() throws Exception {
		
		final Map<String, ?> summary = client.getSummary(null, 
				"fa2adf68-5363-11e6-b0a6-06603eb7f301");
	}
	
	
	@Test
	public void testSave() throws Exception {
		final File inFile = new File("src/test/resources/gal.cx");
		FileInputStream cxis = new FileInputStream(inFile);
		
//		String uuid = client.postNetwork("http://www.ndexbio.org/v2/network", "gal.cx", cxis, "", "");
//		
//		assertNotNull(uuid);
//		System.out.println(uuid);
	}
	

	@Test
	public void testUpdteVisibility() throws Exception {
		
		String url = "http://www.ndexbio.org/v2";
//		client.setVisibility(url, 
//				"5e939fe3-2acb-11e7-8f50-0ac135e8bacf", false, "id", "");
	}
}
