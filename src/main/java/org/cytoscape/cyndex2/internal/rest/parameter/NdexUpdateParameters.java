package org.cytoscape.cyndex2.internal.rest.parameter;

import java.util.Map;

public class NdexUpdateParameters extends NdexSaveParameters{

	public String uuid;

	public NdexUpdateParameters(String userId, String password, String serverUrl, Map<String, String> metadata, boolean writeCollection) {
		super(userId, password, serverUrl, metadata, writeCollection);
	}
}
