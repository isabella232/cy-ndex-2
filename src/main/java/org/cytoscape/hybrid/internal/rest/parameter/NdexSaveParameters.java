package org.cytoscape.hybrid.internal.rest.parameter;

import java.util.Map;

public class NdexSaveParameters {

	public String userId;
	public String password;
	public String serverUrl;
	public String uuid;
	public Map<String, String> metadata;
	public Boolean isPublic;

	public NdexSaveParameters(String userId, String password, String serverUrl, Map<String, String> metadata) {
		this.serverUrl = serverUrl;
		this.password = password;
		this.userId = userId;
		this.metadata = metadata;
		this.isPublic = false;
	}
}
