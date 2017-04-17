package org.cytoscape.hybrid.internal.electron;

public class NdexAppState {

	private String formatVersion;
	private String selectedServerName;

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
}
