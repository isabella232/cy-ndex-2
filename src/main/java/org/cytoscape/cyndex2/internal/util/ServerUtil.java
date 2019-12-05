package org.cytoscape.cyndex2.internal.util;

public class ServerUtil {
	public static final String getDisplayUsernameHTML(final String username) {
		return username != null ? username : "<i>anonymous</i>";
	}
}
