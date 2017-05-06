package org.cytoscape.hybrid.internal.rest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Basic information of CyNDEx-2 app and its REST API.")
public class AppInfoResponse {

	@ApiModelProperty(value = "CyNDEx-2 app version")
	public String appVersion;

	@ApiModelProperty(value = "CyNDEx-2 REST API version")
	public String apiVersion;

	@ApiModelProperty(value = "Official name of this app")
	public String appName;

	@ApiModelProperty(value = "Shoer description of this app")
	public String description;
}
