package org.cytoscape.hybrid.internal.rest;

import java.util.Map;

public class NdexSaveParams {

	private String userId;
	private String password;
	private String serverUrl;
	private String uuid;
	private Map<String, String> metadata;
	private Boolean isPublic;
	

	public NdexSaveParams(String userId, String password, String serverUrl, Map<String, String> metadata) {
		this.serverUrl = serverUrl;
		this.password = password;
		this.userId = userId;
		this.setMetadata(metadata);
		this.setIsPublic(false);
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPassword() {
		return password;
	}

	public void setToken(final String password) {
		this.password = password;
	}

	public String getServerUrl() {
		return serverUrl;
	}

	public void setServerUrl(String serverUrl) {
		this.serverUrl = serverUrl;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}

	public Boolean getIsPublic() {
		return isPublic;
	}

	public void setIsPublic(Boolean isPublic) {
		this.isPublic = isPublic;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
}
