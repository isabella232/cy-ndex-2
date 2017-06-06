package org.cytoscape.hybrid.internal.rest.endpoints.impl;

import javax.ws.rs.core.Response.Status;

import org.cytoscape.ci.CIWrapping;
import org.cytoscape.ci_bridge_impl.CIProvider;
import org.cytoscape.hybrid.internal.rest.endpoints.NdexBaseResource;
import org.cytoscape.hybrid.internal.rest.errors.ErrorBuilder;
import org.cytoscape.hybrid.internal.rest.errors.ErrorType;
import org.cytoscape.hybrid.internal.rest.response.AppInfoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NdexBaseResourceImpl implements NdexBaseResource {

	private static final Logger logger = LoggerFactory.getLogger(NdexBaseResourceImpl.class);

	private static final AppInfoResponse SUMMARY = new AppInfoResponse();
	static {
		SUMMARY.apiVersion = "1";
		SUMMARY.appName = "CyNDEx-2";
		SUMMARY.description = "NDEx client for Cytoscape. This app supports NDEx REST API V2 only.";
	}

	private final ErrorBuilder errorBuilder;

	public NdexBaseResourceImpl(final String bundleVersion, final ErrorBuilder errorBuilder) {
		SUMMARY.appVersion = bundleVersion;
		this.errorBuilder = errorBuilder;
	}

	@Override
	@CIWrapping
	public CIAppInfoResponse getAppInfo() {
		try {
			return CIProvider.getCIResponseFactory().getCIResponse(SUMMARY, CIAppInfoResponse.class);
		} catch (InstantiationException | IllegalAccessException e) {
			final String message = "Could not create wrapped CI JSON.";
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}
}
