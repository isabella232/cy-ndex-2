package org.cytoscape.hybrid.internal.rest;

public class NdexImportParams {

	private String uuid;
	private String userId;
	private String password;
	private String ndexServerUrl;
	
	public NdexImportParams(String uuid, String userId, String password, String ndexServerUrl) {
		this.uuid = uuid;
		this.ndexServerUrl = ndexServerUrl;
		this.password = password;
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
	public String getPassword() {
		return password;
	}
	public void setToken(final String password) {
		this.password = password;
	}
	public String getNdexServerUrl() {
		return ndexServerUrl;
	}
	public void setNdexServerUrl(String ndexServerUrl) {
		this.ndexServerUrl = ndexServerUrl;
	}
}
