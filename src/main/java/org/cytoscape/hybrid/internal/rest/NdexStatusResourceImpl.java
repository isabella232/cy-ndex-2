package org.cytoscape.hybrid.internal.rest;

import java.util.Map;
import java.util.TreeMap;

public class NdexStatusResourceImpl implements NdexStatusResource {

	private final Map<String, String> status;
	
	public NdexStatusResourceImpl() {
		status = new TreeMap<>();
		
		status.put("apiVersion", "v1");
		status.put("appVersion", "2.0.0");
		status.put("appName", "CyNDEx-2");
	}
	
	
	@Override
	public Map<String, String> status() {
		return status;
	}

}
