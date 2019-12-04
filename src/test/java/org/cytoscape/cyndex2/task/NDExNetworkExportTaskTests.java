package org.cytoscape.cyndex2.task;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

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
import org.cytoscape.cyndex2.internal.util.CIServiceManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.TaskMonitor;
import org.junit.Test;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

import com.fasterxml.jackson.core.JsonProcessingException;

public class NDExNetworkExportTaskTests {
	@Test
	public void createNetworkTest() {
		
		final CyNetworkManager networkManager = mock(CyNetworkManager.class);
		
		CyServiceRegistrar reg = mock(CyServiceRegistrar.class);
		when(reg.getService(CyNetworkManager.class)).thenReturn(networkManager);
		CyServiceModule.setServiceRegistrar(reg);
		
		final CyApplicationManager appManager = mock(CyApplicationManager.class);
		
		NDExBasicSaveParameters params = mock(NDExBasicSaveParameters.class);
		final Map<String, String> metadata = new HashMap<String, String>();
		params.metadata = metadata;
		
		CySubNetwork currentSubNetwork = mock(CySubNetwork.class);
		when(currentSubNetwork.getSUID()).thenReturn(669l);
		when(appManager.getCurrentNetwork()).thenReturn(currentSubNetwork);
		
		CyTable subNetworkTable = mock(CyTable.class);
		
		
		CyRow subNetworkRow = mock(CyRow.class);
		when(subNetworkRow.get(CyNetwork.NAME, String.class)).thenReturn("mocksubnetworkname");
		
		when(subNetworkTable.getRow(669l)).thenReturn(subNetworkRow);
		
		when(currentSubNetwork.getRow(currentSubNetwork)).thenReturn(subNetworkRow);
		when(currentSubNetwork.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS)).thenReturn(subNetworkTable);
		
		when(networkManager.getNetwork(669l)).thenReturn(currentSubNetwork);
		
		CyRootNetwork currentRootNetwork = mock(CyRootNetwork.class);
		when(currentRootNetwork.getSUID()).thenReturn(668l);
		when(currentSubNetwork.getRootNetwork()).thenReturn(currentRootNetwork);
		
		CyTable rootNetworkTable = mock(CyTable.class);
		
		CyRow rootNetworkRow = mock(CyRow.class);
		when(rootNetworkRow.get(CyNetwork.NAME, String.class)).thenReturn("mockrootnetworkname");
		
		when(rootNetworkTable.getRow(668l)).thenReturn(rootNetworkRow);
		
		when(currentRootNetwork.getRow(currentRootNetwork)).thenReturn(rootNetworkRow);
		when(currentRootNetwork.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS)).thenReturn(rootNetworkTable);
		
		when(networkManager.getNetwork(668l)).thenReturn(currentRootNetwork);
		
		NdexRestClientModelAccessLayer mal = mock(NdexRestClientModelAccessLayer.class);
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
		} catch (IllegalStateException e) {
			e.printStackTrace();
			fail();
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
}



