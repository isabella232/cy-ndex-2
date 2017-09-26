package org.cytoscape.cyndex2.internal.rest.parameter;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


@ApiModel(description = "Required parameters for loading network(s) from NDEx.")
public class NdexImportParams {

	@ApiModelProperty(value = "URL of NDEx V2 API server")
	public String serverUrl;

	@ApiModelProperty(value = "UUID of the NDEx network")
	public String uuid;
	
	@ApiModelProperty(value = "NDEx user ID")
	public String userId;
	
	@ApiModelProperty(value = "Password for the NDEx account")
	public String password;


	public NdexImportParams(String uuid, String userId, String password, String serverUrl) {
		this.uuid = uuid;
		this.serverUrl = serverUrl;
		this.password = password;
		this.userId = userId;
	}
}
