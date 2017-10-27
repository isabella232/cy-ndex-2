package org.cytoscape.cyndex2.internal.rest;

import java.util.Map;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Summary of a network.")
public class SimpleNetworkSummary {

	@ApiModelProperty(value = "SUID of the current network")
	public Long suid;

	@ApiModelProperty(value = "NDEx UUID of the current network")
	public String uuid;

	@ApiModelProperty(value = "Name of the current network")
	public String name;

	@ApiModelProperty(value = "Network properties (attributes) in current network's table")
	public Map<String, Object> props;

}
