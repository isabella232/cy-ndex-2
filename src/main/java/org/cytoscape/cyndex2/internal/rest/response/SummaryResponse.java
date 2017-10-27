package org.cytoscape.cyndex2.internal.rest.response;

import java.util.Collection;

import org.cytoscape.cyndex2.internal.rest.SimpleNetworkSummary;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Summary of a network collection.")
public class SummaryResponse {
	
	
	@ApiModelProperty(value = "SUID of the current network")
	public Long currentNetworkSuid;
	
	@ApiModelProperty(value = "Summary of the collection (= root network)")
	public SimpleNetworkSummary currentRootNetwork;
	
	@ApiModelProperty(value = "Summary of all networks in the collection")
	public Collection<SimpleNetworkSummary> members;
}
