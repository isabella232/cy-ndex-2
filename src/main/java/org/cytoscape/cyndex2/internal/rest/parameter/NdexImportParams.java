package org.cytoscape.cyndex2.internal.rest.parameter;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


@ApiModel(description = "Required parameters for loading network(s) from NDEx.")
public class NdexImportParams {

	@ApiModelProperty(value = "URL of NDEx V2 API server, defaults to http://ndexbio.org/v2", example="http://ndexbio.org/v2", required=true)
	public String serverUrl;

	@ApiModelProperty(value = "UUID of the NDEx network", example="", required=true)
	public String uuid;
	
	@ApiModelProperty(value = "NDEx username", example="username")
	public String username;
	
	@ApiModelProperty(value = "Password for the NDEx account", example="password")
	public String password;

	@ApiModelProperty(value="NDEx access key", example="", required=false)
	  public String accessKey;
	
	@ApiModelProperty(value="NDEx user's OAuth ID token", example="", required=false)
	public String idToken;
	
	
	public NdexImportParams(String uuid, String username, String password, String serverUrl, String accessKey,String IDToken) {
		this.uuid = uuid;
		this.serverUrl = serverUrl;
		this.password = password;
		this.username = username;
	    this.accessKey = accessKey;
	    this.idToken = IDToken;
	}
}
