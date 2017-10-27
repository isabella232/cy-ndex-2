package org.cytoscape.cyndex2.internal.rest.parameter;

import java.util.Map;

public class NdexSaveParameters {

	public String userId;
	public String password;
	public String serverUrl;
	public Map<String, String> metadata;
	public Boolean isPublic;
	public Boolean writeCollection;

	public NdexSaveParameters(String userId, String password, String serverUrl, Map<String, String> metadata, boolean writeCollection) {
		this.serverUrl = serverUrl;
		this.password = password;
		this.userId = userId;
		this.metadata = metadata;
		this.isPublic = false;
		this.writeCollection = writeCollection;
	}
}
