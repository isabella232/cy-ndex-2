package org.cytoscape.cyndex2.internal.rest.parameter;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


@ApiModel(description = "Required parameters for loading network(s) from NDEx.")
public class NdexImportParams {

	@ApiModelProperty(value = "URL of NDEx V2 API server", example="http://ndexbio.org/v2", required=true)
	public String serverUrl;

	@ApiModelProperty(value = "UUID of the NDEx network", example="", required=true)
	public String uuid;
	
	@ApiModelProperty(value = "NDEx username", example="username")
	public String username;
	
	@ApiModelProperty(value = "Password for the NDEx account", example="password")
	public String password;


	public NdexImportParams(String uuid, String username, String password, String serverUrl) {
		this.uuid = uuid;
		this.serverUrl = serverUrl;
		this.password = password;
		this.username = username;
	}
}
