package org.cytoscape.hybrid.internal.electron;

import java.util.Map;

import org.cytoscape.hybrid.internal.login.Credential;

public class NdexAppState {

	private String formatVersion;
	private String selectedServerName;
	private Map<String, Credential> servers;

	public String getFormatVersion() {
		return formatVersion;
	}

	public void setFormatVersion(String formatVersion) {
		this.formatVersion = formatVersion;
	}

	public String getSelectedServerName() {
		return selectedServerName;
	}

	public void setSelectedServerName(String selectedServerName) {
		this.selectedServerName = selectedServerName;
	}

	public Map<String, Credential> getServers() {
		return servers;
	}

	public void setServers(Map<String, Credential> servers) {
		this.servers = servers;
	}
}
