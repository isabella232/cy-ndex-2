package org.cytoscape.cyndex2.internal.rest.parameter;

import java.util.Map;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Required parameters for saving network(s) to NDEx.")
public class NdexSaveParameters {
	@ApiModelProperty(value = "NDEx username", example="username", required=true)
	public String username;
	@ApiModelProperty(value = "Password for the NDEx account", example="password", required=true)
	public String password;
	@ApiModelProperty(value = "URL of NDEx V2 API server", example="http://ndexbio.org/v2", required=true)
	public String serverUrl;
	@ApiModelProperty(value = "Network metadata", required=true)
	public Map<String, String> metadata;
	@ApiModelProperty(value = "Visibility of network", example="true", required=true)
	public Boolean isPublic;

	private NdexSaveParameters(String username, String password, String serverUrl, Map<String, String> metadata, boolean writeCollection) {
		this.serverUrl = serverUrl;
		this.password = password;
		this.username = username;
		this.metadata = metadata;
		this.isPublic = false;
	}
}
