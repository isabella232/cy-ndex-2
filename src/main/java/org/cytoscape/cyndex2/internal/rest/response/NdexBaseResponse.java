package org.cytoscape.cyndex2.internal.rest.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Response for import API call.")
public final class NdexBaseResponse {

	@ApiModelProperty(value = "Cytoscape session-unique ID (SUID) of the network")
	public Long suid;
	
	@ApiModelProperty(value = "NDEx network UUID")
	public String uuid;

	public NdexBaseResponse(final Long suid, final String uuid) {
		this.suid = suid;
		this.uuid = uuid;
	}
}
