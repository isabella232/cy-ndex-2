package org.cytoscape.hybrid.internal.rest;

public class NdexImportParams {

	private String uuid;
	private String userId;
	private String token;
	private String ndexServerUrl;
	
	public NdexImportParams(String uuid, String userId, String token, String ndexServerUrl) {
		this.uuid = uuid;
		this.ndexServerUrl = ndexServerUrl;
		this.token = token;
		this.userId = userId;
	}
	
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public String getNdexServerUrl() {
		return ndexServerUrl;
	}
	public void setNdexServerUrl(String ndexServerUrl) {
		this.ndexServerUrl = ndexServerUrl;
	}
}
