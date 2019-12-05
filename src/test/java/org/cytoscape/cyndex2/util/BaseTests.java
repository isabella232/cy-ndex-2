package org.cytoscape.cyndex2.util;

import static org.junit.Assert.assertEquals;

import org.cytoscape.cyndex2.internal.util.ServerList;
import org.junit.Test;

public class BaseTests {
	@Test
	public void verifyServerKey() {
		assertEquals("ServerList key has been changed. This will make previously saved user profiles invalid.", "YlKMdJocN6eNNlCY", ServerList.KEY);
	}
}



