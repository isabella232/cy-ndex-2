package org.cytoscape.cyndex2.internal.rest.parameter;

import java.util.Map;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Required parameters for saving network(s) to NDEx.")
public class NdexSaveParameters extends NdexBasicSaveParameter {
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
