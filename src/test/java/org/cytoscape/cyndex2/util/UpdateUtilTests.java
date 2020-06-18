package org.cytoscape.cyndex2.util;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.UUID;

import org.cytoscape.cyndex2.internal.CyServiceModule;
import org.cytoscape.cyndex2.internal.util.UpdateUtil;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.junit.Test;
import org.mockito.Mockito;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Permissions;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

import java.util.Map;

public class UpdateUtilTests {
	
	@Test (expected = Exception.class)
	public void updateWhenNoSessionModifiedTime() throws Exception { 
		CyServiceRegistrar reg = mock(CyServiceRegistrar.class);
		CyServiceModule.setServiceRegistrar(reg);
		
		UUID uuid = new UUID(1l,2l);
		UUID user = new UUID(1l,3l);
		
		CyNetwork network = mock(CyNetwork.class);
		when(network.getSUID()).thenReturn(669l);
		
		CyRow networkRow = mock(CyRow.class);
		when(networkRow.get("NDEx UUID", String.class)).thenReturn((new UUID(1l,2l)).toString());
		
		CyTable networkTable = mock(CyTable.class);
		when(network.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS)).thenReturn(networkTable);
		when(networkTable.getRow(669l)).thenReturn(networkRow);

		CyNetworkManager nm = mock(CyNetworkManager.class);
		when(nm.getNetwork(669l)).thenReturn(network);
		when(reg.getService(CyNetworkManager.class)).thenReturn(nm);
		
		NdexRestClient nc = mock(NdexRestClient.class);
		when(nc.getUserUid()).thenReturn(user);
		NdexRestClientModelAccessLayer mal = mock(NdexRestClientModelAccessLayer.class);
		
		Map<String, Permissions> permissionTable = new HashMap<String, Permissions>();
		permissionTable.put(user.toString(), Permissions.WRITE);
		
		NetworkSummary ns = mock(NetworkSummary.class);
		
		when(ns.getModificationTime()).thenReturn(new Timestamp(0));
		when(ns.getIsReadOnly()).thenReturn(false);
		
		try {
			when(mal.getNetworkSummaryById(uuid)).thenReturn(ns);
		} catch (IOException | NdexException e2) {
			e2.printStackTrace();
			fail();
		}
	
		
		try {
			when(mal.getUserNetworkPermission(any(UUID.class), any(UUID.class),
					Mockito.anyBoolean())).thenReturn(permissionTable);
		} catch (IOException | NdexException e1) {
			e1.printStackTrace();
		}
		
		UpdateUtil.updateIsPossibleHelper(669l, false, nc, mal);
		 
		fail("UpdateUtil did not throw expected exception");
	}
	
	@Test (expected = Exception.class)
	public void updateWhenSessionIsOutdated() throws Exception { 
		CyServiceRegistrar reg = mock(CyServiceRegistrar.class);
		CyServiceModule.setServiceRegistrar(reg);
		
		UUID uuid = new UUID(1l,2l);
		UUID user = new UUID(1l,3l);
		
		CyNetwork network = mock(CyNetwork.class);
		when(network.getSUID()).thenReturn(669l);
		
		CyRow networkRow = mock(CyRow.class);
		when(networkRow.get("NDEx UUID", String.class)).thenReturn((new UUID(1l,2l)).toString());
		when(networkRow.get("NDEx Modification Timestamp", String.class)).thenReturn((new Timestamp(0)).toString());
		
		
		CyTable networkTable = mock(CyTable.class);
		when(network.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS)).thenReturn(networkTable);
		when(networkTable.getRow(669l)).thenReturn(networkRow);

		CyNetworkManager nm = mock(CyNetworkManager.class);
		when(nm.getNetwork(669l)).thenReturn(network);
		when(reg.getService(CyNetworkManager.class)).thenReturn(nm);
		
		NdexRestClient nc = mock(NdexRestClient.class);
		when(nc.getUserUid()).thenReturn(user);
		NdexRestClientModelAccessLayer mal = mock(NdexRestClientModelAccessLayer.class);
		
		Map<String, Permissions> permissionTable = new HashMap<String, Permissions>();
		permissionTable.put(user.toString(), Permissions.WRITE);
		
		NetworkSummary ns = mock(NetworkSummary.class);
		
		when(ns.getModificationTime()).thenReturn(new Timestamp(1));
		when(ns.getIsReadOnly()).thenReturn(false);
		
		try {
			when(mal.getNetworkSummaryById(uuid)).thenReturn(ns);
		} catch (IOException | NdexException e2) {
			e2.printStackTrace();
			fail();
		}
	
		
		try {
			when(mal.getUserNetworkPermission(any(UUID.class), any(UUID.class),
					Mockito.anyBoolean())).thenReturn(permissionTable);
		} catch (IOException | NdexException e1) {
			e1.printStackTrace();
		}
		
		UpdateUtil.updateIsPossibleHelper(669l, false, nc, mal);
		 
		fail("UpdateUtil did not throw expected exception");
	}
	
	@Test 
	public void updateWhenModifiable() throws Exception { 
		CyServiceRegistrar reg = mock(CyServiceRegistrar.class);
		CyServiceModule.setServiceRegistrar(reg);
		
		UUID uuid = new UUID(1l,2l);
		UUID user = new UUID(1l,3l);
		
		CyNetwork network = mock(CyNetwork.class);
		when(network.getSUID()).thenReturn(669l);
		
		CyRow networkRow = mock(CyRow.class);
		when(networkRow.get("NDEx UUID", String.class)).thenReturn((new UUID(1l,2l)).toString());
		when(networkRow.get("NDEx Modification Timestamp", String.class)).thenReturn((new Timestamp(0l)).toString());
		
		
		CyTable networkTable = mock(CyTable.class);
		when(network.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS)).thenReturn(networkTable);
		when(networkTable.getRow(669l)).thenReturn(networkRow);

		CyNetworkManager nm = mock(CyNetworkManager.class);
		when(nm.getNetwork(669l)).thenReturn(network);
		when(reg.getService(CyNetworkManager.class)).thenReturn(nm);
		
		NdexRestClient nc = mock(NdexRestClient.class);
		when(nc.getUserUid()).thenReturn(user);
		NdexRestClientModelAccessLayer mal = mock(NdexRestClientModelAccessLayer.class);
		
		Map<String, Permissions> permissionTable = new HashMap<String, Permissions>();
		permissionTable.put(user.toString(), Permissions.WRITE);
		
		NetworkSummary ns = mock(NetworkSummary.class);
		
		when(ns.getModificationTime()).thenReturn(new Timestamp(0));
		when(ns.getIsReadOnly()).thenReturn(false);
		
		try {
			when(mal.getNetworkSummaryById(uuid)).thenReturn(ns);
		} catch (IOException | NdexException e2) {
			e2.printStackTrace();
			fail();
		}
	
		
		try {
			when(mal.getUserNetworkPermission(any(UUID.class), any(UUID.class),
					Mockito.anyBoolean())).thenReturn(permissionTable);
		} catch (IOException | NdexException e1) {
			e1.printStackTrace();
		}
		
		UpdateUtil.updateIsPossibleHelper(669l, false, nc, mal);
		 
		
	}
	
}



