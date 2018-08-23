package org.cytoscape.cyndex2.internal.rest.parameter;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "Required parameters for saving network(s) to NDEx.")
public class NdexSaveParameters extends NdexBasicSaveParameter {
	@ApiModelProperty(value = "Visibility of network", example="true")
	public Boolean isPublic = true;

}
