package org.cytoscape.hybrid.internal.rest;

import java.util.Collection;

public class SummaryResponse {
	
	public Long currentNetworkSUID;
	public Long currentRootNetworkSUID;
	public Collection<Long> memberSUIDs;
	
	
	public SummaryResponse(Long currentNetworkSUID, Long currentRootNetworkSUID, Collection<Long> memberSUIDs) {
	
		this.currentNetworkSUID = currentNetworkSUID;
		this.currentRootNetworkSUID = currentRootNetworkSUID;
		this.memberSUIDs = memberSUIDs;
	}
}
