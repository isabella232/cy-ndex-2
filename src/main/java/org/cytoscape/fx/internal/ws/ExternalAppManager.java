package org.cytoscape.fx.internal.ws;

public class ExternalAppManager {

	private Process currentProcess;

	private String query;

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
}
