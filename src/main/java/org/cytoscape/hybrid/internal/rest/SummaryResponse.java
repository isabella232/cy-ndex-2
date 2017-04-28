package org.cytoscape.hybrid.internal.rest;

import java.util.Collection;

public class SummaryResponse {
	
	public Long currentNetworkSuid;
	public NetworkSummary currentRootNetwork;
	public Collection<NetworkSummary> members;
}
