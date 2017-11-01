package org.cytoscape.cyndex2.internal.rest.endpoints.impl;

import javax.ws.rs.core.Response.Status;

import org.cytoscape.ci.CIWrapping;
import org.cytoscape.ci_bridge_impl.CIProvider;
import org.cytoscape.cyndex2.internal.rest.endpoints.NdexStatusResource;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorBuilder;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorType;
import org.cytoscape.cyndex2.internal.rest.parameter.AppStatusParameters;
import org.cytoscape.cyndex2.internal.rest.parameter.LoadParameters;
import org.cytoscape.cyndex2.internal.rest.response.AppStatusResponse;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NdexStatusResourceImpl implements NdexStatusResource {

	private static final Logger logger = LoggerFactory.getLogger(NdexStatusResourceImpl.class);
	
	private final ExternalAppManager pm;
	private final ErrorBuilder errorBuilder;
	private AppStatusResponse<AppStatusParameters> status;

	public NdexStatusResourceImpl(final ExternalAppManager pm, ErrorBuilder errorBuilder) {
		this.pm = pm;
		this.errorBuilder = errorBuilder;
	}

	private AppStatusParameters setLoadProps() {
		final LoadParameters loadParameters = new LoadParameters();
		loadParameters.searchTerm = pm.getQuery();
		return loadParameters;
	}

	@Override
	@CIWrapping
	public CIAppStatusResponse getAppStatus() {

		final String widget = pm.getAppName();
		if (widget == null) {
			final String message = "Application type is not set yet.";
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}

		status = new AppStatusResponse<>();

		if ( widget.equals(ExternalAppManager.APP_NAME_LOAD)) {
			status.parameters = setLoadProps();
			status.widget = "choose";
		} else {
			status.widget = "save";
			status.parameters = new AppStatusParameters() {
			};
		}
		
		try {
			return CIProvider.getCIResponseFactory().getCIResponse(status, CIAppStatusResponse.class);
		} catch (InstantiationException | IllegalAccessException e) {
			final String message = "Could not create wrapped CI JSON.";
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}
	}
}
