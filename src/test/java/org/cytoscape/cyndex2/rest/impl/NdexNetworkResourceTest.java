package org.cytoscape.cyndex2.rest.impl;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.ci.CIResponseFactory;
import org.cytoscape.cyndex2.internal.rest.NdexClient;
import org.cytoscape.cyndex2.internal.rest.endpoints.NdexNetworkResource.CISummaryResponse;
import org.cytoscape.cyndex2.internal.rest.endpoints.impl.NdexNetworkResourceImpl;
import org.cytoscape.cyndex2.internal.util.CIServiceManager;
import org.cytoscape.ding.NetworkViewTestSupport;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NdexNetworkResourceTest {
	
	protected NetworkTestSupport nts = new NetworkTestSupport();
	protected NetworkViewTestSupport nvts = new NetworkViewTestSupport();
	
	@Test
	public void testGetCurrentNetworkSummary() {
		final NdexClient client = mock(NdexClient.class);
		final CyApplicationManager appManager = mock(CyApplicationManager.class);
		final CyNetworkManager networkManager = mock(CyNetworkManager.class);
		final CIServiceManager ciServiceTracker = mock(CIServiceManager.class);

		CIResponseFactory ciResponseFactory = mock(CIResponseFactory.class);
		
		//when(ciResponseFactory.getCIResponse(data, CISummaryResponse.class))
		try {
			doAnswer(new Answer<CISummaryResponse>() {
				public CISummaryResponse answer(InvocationOnMock invocation) {
					return null;
				}
			}).when(ciResponseFactory).getCIResponse(org.mockito.Matchers.any(), eq(CISummaryResponse.class));
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
		when(ciServiceTracker.getCIResponseFactory()).thenReturn(ciResponseFactory);
		
		CySubNetwork currentSubNetwork = mock(CySubNetwork.class);
		CyRootNetwork currentRootNetwork = mock(CyRootNetwork.class);
		when(currentRootNetwork.getSUID()).thenReturn(668l);
		
		CyRow rootNetworkRow = mock(CyRow.class);
		CyTable rootNetworkTable = mock(CyTable.class);
		
		when(rootNetworkTable.getRow(668l)).thenReturn(rootNetworkRow);
		when(currentRootNetwork.getDefaultNetworkTable()).thenReturn(rootNetworkTable);
		when(currentRootNetwork.getTable(CyNetwork.class, CyNetwork.LOCAL_ATTRS)).thenReturn(rootNetworkTable);
		
		CyRow hiddenRow = mock(CyRow.class);
		CyTable hiddenTable = mock(CyTable.class);
		
		when(currentRootNetwork.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS)).thenReturn(hiddenTable);
		
		when(currentSubNetwork.getRootNetwork()).thenReturn(currentRootNetwork);
		when(currentSubNetwork.getSUID()).thenReturn(669l);
		when(appManager.getCurrentNetwork()).thenReturn(currentSubNetwork);
		
		NdexNetworkResourceImpl impl = new NdexNetworkResourceImpl(client, appManager, networkManager, ciServiceTracker);
		impl.getCurrentNetworkSummary();
		
	}
	
	
}



