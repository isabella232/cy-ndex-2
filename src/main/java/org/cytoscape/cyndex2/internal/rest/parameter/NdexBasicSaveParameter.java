package org.cytoscape.cyndex2.internal.rest.parameter;

import java.util.Map;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Required parameters for updading network(s) to NDEx.")
public class NdexBasicSaveParameter {

	@ApiModelProperty(value = "NDEx username", example = "username", required = true)
	public String username;
	@ApiModelProperty(value = "Password for the NDEx account", example = "password", required = true)
	public String password;
	@ApiModelProperty(value = "URL of NDEx V2 API server", example = "http://ndexbio.org/v2", required = true)
	public String serverUrl;
	@ApiModelProperty(value = "Network metadata", required = true)
	public Map<String, String> metadata;

	public NdexBasicSaveParameter() {
		super();
	}

}