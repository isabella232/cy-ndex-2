package org.cytoscape.cyndex2.task;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.cyndex2.internal.CxTaskFactoryManager;
import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.CyServiceModule;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExBasicSaveParameters;
import org.cytoscape.cyndex2.internal.task.NetworkExportTask.NetworkExportException;
import org.cytoscape.cyndex2.internal.task.NetworkImportTask;
import org.cytoscape.cyndex2.internal.task.NetworkImportTask.NetworkImportException;
import org.cytoscape.io.read.AbstractCyNetworkReader;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

public class NDExNetworkImportTaskTests {
	
	CyServiceRegistrar reg;
	CyNetworkManager networkManager;
	
	
	InputStreamTaskFactory inputStreamTaskFactory;
	AbstractCyNetworkReader readerTask;
	CyNetwork cyNetwork;
	CyTable cyTable;
	
	TaskIterator taskIterator;
	
	InputStream inputStream;
	NdexRestClientModelAccessLayer mal;
	
	@Before
	public void createMocks() {
		reg = mock(CyServiceRegistrar.class);
		networkManager = mock(CyNetworkManager.class);
		when(reg.getService(CyNetworkManager.class)).thenReturn(networkManager);
		
		/*
		CyProperty cyProps = mock(CyProperty.class);
		
		Properties props = mock(Properties.class);
		when(props.getProperty(Mockito.eq(NetworkImportTask.VIEW_THRESHOLD))).thenReturn("3000");
		
		when(cyProps.getProperties()).thenReturn(props);
		when(reg.getService(Mockito.eq(CyProperty.class), Mockito.eq("(cyPropertyName=cytoscape3.props)"))).thenReturn(cyProps);
		*/
		
		CyServiceModule.setServiceRegistrar(reg);
		
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("id", "cytoscapeCxNetworkReaderFactory");
		
		inputStreamTaskFactory = mock(InputStreamTaskFactory.class);
		readerTask = mock(AbstractCyNetworkReader.class); 
		cyNetwork = mock(CyNetwork.class);
		when(cyNetwork.getSUID()).thenReturn(668l);
		
		cyTable = mock(CyTable.class);
		when(cyTable.getRow(668l)).thenReturn(mock(CyRow.class));
		when(cyNetwork.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS)).thenReturn(cyTable);
		
		CyNetwork[] cyNetworks = {cyNetwork};
		when(readerTask.getNetworks()).thenReturn(cyNetworks);
		
		taskIterator = new TaskIterator();
		taskIterator.append(readerTask);
		
		when(inputStreamTaskFactory.createTaskIterator(any(InputStream.class), anyObject())).thenReturn(taskIterator);
		
		CxTaskFactoryManager.INSTANCE.addReaderFactory(inputStreamTaskFactory, properties);
		
		inputStream = mock(InputStream.class);
		mal = mock(NdexRestClientModelAccessLayer.class);
	}
	
	@Test
	public void importNetworkWithAccessKeyTest() {
		try {
			NetworkSummary networkSummary = mock(NetworkSummary.class);
			when(networkSummary.getExternalId()).thenReturn(new UUID(1l,2l));
			when(mal.getNetworkSummaryById(new UUID(1l,2l), "mockAccessKey")).thenReturn(networkSummary);
				when(mal.getNetworkAsCXStream(new UUID(1l,2l), "mockAccessKey")).thenReturn(inputStream);
			NetworkImportTask task = new NetworkImportTask(mal, new UUID(1l,2l), "mockAccessKey");
			TaskMonitor taskMonitor = mock(TaskMonitor.class);
			task.run(taskMonitor);
		} catch (NetworkExportException | NetworkImportException | IOException | NdexException e) {
			e.printStackTrace();
			fail();
		}
		
		try {
			verify(mal).getNetworkAsCXStream(new UUID(1l,2l), "mockAccessKey");
		} catch (IOException | NdexException e) {
			e.printStackTrace();
			fail();
		}
		
	}
	
	@Test
	public void importNetworkTest() {
		try {
			NetworkSummary networkSummary = mock(NetworkSummary.class);
			when(networkSummary.getExternalId()).thenReturn(new UUID(1l,2l));
			when(mal.getNetworkSummaryById(new UUID(1l,2l), null)).thenReturn(networkSummary);
				when(mal.getNetworkAsCXStream(new UUID(1l,2l))).thenReturn(inputStream);
			NetworkImportTask task = new NetworkImportTask(mal, new UUID(1l,2l), null);
			TaskMonitor taskMonitor = mock(TaskMonitor.class);
			task.run(taskMonitor);
		} catch (NetworkExportException | NetworkImportException | IOException | NdexException e) {
			e.printStackTrace();
			fail();
		}
		
		try {
			verify(mal).getNetworkAsCXStream(new UUID(1l,2l));
		} catch (IOException | NdexException e) {
			e.printStackTrace();
			fail();
		}
		
	}
}



