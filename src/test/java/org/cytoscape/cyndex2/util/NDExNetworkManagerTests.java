package org.cytoscape.cyndex2.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.cytoscape.cyndex2.internal.util.NDExNetworkManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.junit.Test;

import static org.mockito.Mockito.verify;

public class NDExNetworkManagerTests {
	@Test
	public void getUUIDTest() {
		CyNetwork network = mock(CyNetwork.class);
		when(network.getSUID()).thenReturn(669l);
		
		CyRow networkRow = mock(CyRow.class);
		when(networkRow.get("NDEx UUID", String.class)).thenReturn((new UUID(1l,2l)).toString());
		
		CyTable networkTable = mock(CyTable.class);
		when(network.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS)).thenReturn(networkTable);
		
		when(networkTable.getRow(669l)).thenReturn(networkRow);
		
		UUID uuid = NDExNetworkManager.getUUID(network);
		assertEquals(new UUID(1l,2l), uuid);
	}
	
	@Test
	public void saveUUIDTest() {
		UUID uuid = new UUID(1l,2l);
		
		CyNetwork network = mock(CyNetwork.class);
		when(network.getSUID()).thenReturn(669l);
		
		CyRow networkRow = mock(CyRow.class);
		when(networkRow.get("NDEx UUID", String.class)).thenReturn((new UUID(1l,2l)).toString());
		
		CyTable networkTable = mock(CyTable.class);
		when(network.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS)).thenReturn(networkTable);
		
		when(networkTable.getRow(669l)).thenReturn(networkRow);
		
		NDExNetworkManager.saveUUID(network, uuid, null);
		verify(networkTable).getColumn("NDEx UUID");
		verify(networkTable).createColumn("NDEx UUID", String.class, false);
		verify(networkRow).set("NDEx UUID", uuid.toString());
		
	}
	
}



