package org.cytoscape.cyndex2.task;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.cyndex2.internal.CyServiceModule;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExBasicSaveParameters;
import org.cytoscape.cyndex2.internal.task.NetworkExportTask;
import org.cytoscape.cyndex2.internal.task.NetworkExportTask.NetworkExportException;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.TaskMonitor;
import org.junit.Before;
import org.junit.Test;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

import com.fasterxml.jackson.core.JsonProcessingException;

public class NDExNetworkExportTaskTests {
	
	final UUID subNetworkUUID = new UUID(1,2);
	final UUID rootNetworkUUID = new UUID(2,3);
	CyNetworkManager networkManager;
	CyServiceRegistrar reg;
	CyApplicationManager appManager;
	
	CySubNetwork currentSubNetwork;
	CyTable subNetworkTable;
	CyRow subNetworkRow;
	
	CyRootNetwork currentRootNetwork;
	CyTable rootNetworkTable;
	CyRow rootNetworkRow;
	
	NdexRestClientModelAccessLayer mal;
	
	@Before
	public void createMocks() {
	  networkManager = mock(CyNetworkManager.class);
		reg = mock(CyServiceRegistrar.class);
		
		when(reg.getService(CyNetworkManager.class)).thenReturn(networkManager);
		CyServiceModule.setServiceRegistrar(reg);
		
		appManager = mock(CyApplicationManager.class);
		
		currentSubNetwork = mock(CySubNetwork.class);
		when(currentSubNetwork.getSUID()).thenReturn(669l);
		when(appManager.getCurrentNetwork()).thenReturn(currentSubNetwork);
		
		subNetworkTable = mock(CyTable.class);
		
		subNetworkRow = mock(CyRow.class);
		when(subNetworkRow.get(CyNetwork.NAME, String.class)).thenReturn("mocksubnetworkname");
		when(subNetworkRow.get("NDEx UUID", String.class)).thenReturn(subNetworkUUID.toString());
		
		when(subNetworkTable.getRow(669l)).thenReturn(subNetworkRow);
		
		when(currentSubNetwork.getRow(currentSubNetwork)).thenReturn(subNetworkRow);
		when(currentSubNetwork.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS)).thenReturn(subNetworkTable);
		
		when(networkManager.getNetwork(669l)).thenReturn(currentSubNetwork);
		
		currentRootNetwork = mock(CyRootNetwork.class);
		when(currentRootNetwork.getSUID()).thenReturn(668l);
		when(currentSubNetwork.getRootNetwork()).thenReturn(currentRootNetwork);
		
		rootNetworkTable = mock(CyTable.class);
		
		rootNetworkRow = mock(CyRow.class);
		when(rootNetworkRow.get(CyNetwork.NAME, String.class)).thenReturn("mockrootnetworkname");
		when(rootNetworkRow.get("NDEx UUID", String.class)).thenReturn(rootNetworkUUID.toString());
		
		when(rootNetworkTable.getRow(668l)).thenReturn(rootNetworkRow);
		
		when(currentRootNetwork.getRow(currentRootNetwork)).thenReturn(rootNetworkRow);
		when(currentRootNetwork.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS)).thenReturn(rootNetworkTable);
		
		when(networkManager.getNetwork(668l)).thenReturn(currentRootNetwork);
		mal = mock(NdexRestClientModelAccessLayer.class);
	}
	
	@Test
	public void createNetworkTest() {
		NDExBasicSaveParameters params = mock(NDExBasicSaveParameters.class);
		Map<String, String> metadata = new HashMap<String, String>();
		params.metadata = metadata;
		try {
			when(mal.createCXNetwork(any(InputStream.class))).thenReturn(new UUID(1l,2l));
		} catch (Exception e1) {
			e1.printStackTrace();
			fail();
		}
		
		InputStream inputStream = mock(InputStream.class);
		
		try {
			NetworkExportTask task = new NetworkExportTask(mal, 669l, inputStream, params, false, false);
			TaskMonitor taskMonitor = mock(TaskMonitor.class);
			task.run(taskMonitor);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			fail();
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		} catch (NdexException e) {
			e.printStackTrace();
			fail();
		} catch (NetworkExportException e) {
			e.printStackTrace();
			fail();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			fail();
		} catch (InterruptedException e) {
			e.printStackTrace();
			fail();
		}
		try {
			verify(mal).createCXNetwork(inputStream);
			verify(mal, never()).updateCXNetwork(any(UUID.class), any(InputStream.class));
		} catch (IllegalStateException e) {
			e.printStackTrace();
			fail();
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	@Test
	public void createCollectionTest() {
		NDExBasicSaveParameters params = mock(NDExBasicSaveParameters.class);
		Map<String, String> metadata = new HashMap<String, String>();
		params.metadata = metadata;
		try {
			when(mal.createCXNetwork(any(InputStream.class))).thenReturn(new UUID(1l,2l));
		} catch (Exception e1) {
			e1.printStackTrace();
			fail();
		}
		
		InputStream inputStream = mock(InputStream.class);
		
		try {
			NetworkExportTask task = new NetworkExportTask(mal, 669l, inputStream, params, true, false);
			TaskMonitor taskMonitor = mock(TaskMonitor.class);
			task.run(taskMonitor);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			fail();
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		} catch (NdexException e) {
			e.printStackTrace();
			fail();
		} catch (NetworkExportException e) {
			e.printStackTrace();
			fail();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			fail();
		} catch (InterruptedException e) {
			e.printStackTrace();
			fail();
		}
		try {
			verify(mal).createCXNetwork(inputStream);
			verify(mal, never()).updateCXNetwork(any(UUID.class), any(InputStream.class));
		} catch (IllegalStateException e) {
			e.printStackTrace();
			fail();
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	@Test
	public void updateNetworkTest() {
		NDExBasicSaveParameters params = mock(NDExBasicSaveParameters.class);
		Map<String, String> metadata = new HashMap<String, String>();
		params.metadata = metadata;
		try {
			when(mal.createCXNetwork(any(InputStream.class))).thenReturn(new UUID(1l,2l));
		} catch (Exception e1) {
			e1.printStackTrace();
			fail();
		}
		
		InputStream inputStream = mock(InputStream.class);
		
		try {
			NetworkExportTask task = new NetworkExportTask(mal, 669l, inputStream, params, false, true);
			TaskMonitor taskMonitor = mock(TaskMonitor.class);
			task.run(taskMonitor);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			fail();
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		} catch (NdexException e) {
			e.printStackTrace();
			fail();
		} catch (NetworkExportException e) {
			e.printStackTrace();
			fail();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			fail();
		} catch (InterruptedException e) {
			e.printStackTrace();
			fail();
		}
		try {
			verify(mal).updateCXNetwork(subNetworkUUID, inputStream);
			verify(mal, never()).createCXNetwork(any(InputStream.class));
		} catch (IllegalStateException e) {
			e.printStackTrace();
			fail();
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	@Test
	public void updateCollectionTest() {
		NDExBasicSaveParameters params = mock(NDExBasicSaveParameters.class);
		Map<String, String> metadata = new HashMap<String, String>();
		params.metadata = metadata;
		try {
			when(mal.createCXNetwork(any(InputStream.class))).thenReturn(new UUID(1l,2l));
		} catch (Exception e1) {
			e1.printStackTrace();
			fail();
		}
		
		InputStream inputStream = mock(InputStream.class);
		
		try {
			NetworkExportTask task = new NetworkExportTask(mal, 669l, inputStream, params, true, true);
			TaskMonitor taskMonitor = mock(TaskMonitor.class);
			task.run(taskMonitor);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			fail();
		} catch (IOException e) {
			e.printStackTrace();
			fail();
		} catch (NdexException e) {
			e.printStackTrace();
			fail();
		} catch (NetworkExportException e) {
			e.printStackTrace();
			fail();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			fail();
		} catch (InterruptedException e) {
			e.printStackTrace();
			fail();
		}
		try {
			verify(mal).updateCXNetwork(rootNetworkUUID, inputStream);
			verify(mal, never()).createCXNetwork(any(InputStream.class));
		} catch (IllegalStateException e) {
			e.printStackTrace();
			fail();
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
}



