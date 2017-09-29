package org.cytoscape.cyndex2.internal.util;

public class ExternalAppManager {

	public static final String APP_NAME_LOAD = "choose";
	public static final String APP_NAME_SAVE = "save";

	private String port;
	private String query;
	private String appName;

	public void setQuery(String query) {
		this.query = query;
	}

	public String getQuery() {
		return query;
	}
	public void setPort(String port) {
		this.port = port;
	}

	public String getPort() {
		return port;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}
}
