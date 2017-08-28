package org.cytoscape.cyndex2.internal.util;

public class ExternalAppManager {

	public static final String APP_NAME_LOAD = "choose";
	public static final String APP_NAME_SAVE = "save";
	
	private Process currentProcess;
	
	private String query;
	
	private String appName;
	

	public void setProcess(final Process process) {
		this.currentProcess = process;
	}

	public void kill() {
		if (currentProcess != null) {
			this.currentProcess.destroyForcibly();
			this.currentProcess = null;
		}
	}

	public Boolean isActive() {
		if (this.currentProcess != null && this.currentProcess.isAlive()) {
			return true;
		} else {
			return false;
		}
	}

	public void setQuery(final String query) {
		this.query = query;
	}
	
	public String getQuery() {
		return this.query;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}
}
