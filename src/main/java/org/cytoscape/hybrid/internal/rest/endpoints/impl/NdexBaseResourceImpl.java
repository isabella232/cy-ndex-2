package org.cytoscape.hybrid.internal.rest.endpoints.impl;

import org.cytoscape.ci.CIWrapping;
import org.cytoscape.hybrid.internal.rest.AppInfoResponse;
import org.cytoscape.hybrid.internal.rest.endpoints.NdexBaseResource;

public class NdexBaseResourceImpl implements NdexBaseResource {
	
	private static final AppInfoResponse SUMMARY = new AppInfoResponse();
	
	static {
		SUMMARY.appVersion =  "2.0.1";
		SUMMARY.apiVersion = "1";
		SUMMARY.appName = "CyNDEx-2";
		SUMMARY.description = "NDEx client for Cytoscape. This app supports NDEx API V2.";
	}
	

	@Override
	@CIWrapping
	public AppInfoResponse getAppInfo() {
		return SUMMARY;
	}

}
