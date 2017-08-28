package org.cytoscape.cyndex2.internal.rest.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Status of the CyNDEx-2 application.")
public class AppStatusResponse <T> {
	
	@ApiModelProperty(
			value = "Current application type opened by CyNDEx app",
			required = true,
			allowableValues = "choose,save")
	public String widget;
	
	@ApiModelProperty(
			value = "Parameters for NDEx save and load",
			required = true)
	public T parameters;

}
