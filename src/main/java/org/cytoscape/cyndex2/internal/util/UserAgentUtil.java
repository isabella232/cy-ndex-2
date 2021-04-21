package org.cytoscape.cyndex2.internal.util;

import org.cytoscape.cyndex2.internal.CyActivator;

public class UserAgentUtil {
	public static String getCyNDExUserAgent() {
		return CyActivator.getAppName() + "/" + CyActivator.getAppVersion();
	}

	public static String getCytoscapeUserAgent() {
		return "Cytoscape/" + CyActivator.getCytoscapeVersion();
	}
}
