package org.cytoscape.hybrid.internal.rest.endpoints.impl;

import javax.ws.rs.core.Response.Status;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.ci.CIWrapping;
import org.cytoscape.ci_bridge_impl.CIProvider;
import org.cytoscape.hybrid.internal.rest.endpoints.NdexStatusResource;
import org.cytoscape.hybrid.internal.rest.errors.ErrorBuilder;
import org.cytoscape.hybrid.internal.rest.errors.ErrorType;
import org.cytoscape.hybrid.internal.rest.parameter.AppStatusParameters;
import org.cytoscape.hybrid.internal.rest.parameter.LoadParameters;
import org.cytoscape.hybrid.internal.rest.parameter.SaveParameters;
import org.cytoscape.hybrid.internal.rest.response.AppStatusResponse;
import org.cytoscape.hybrid.internal.ws.ExternalAppManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NdexStatusResourceImpl implements NdexStatusResource {

	private static final Logger logger = LoggerFactory.getLogger(NdexStatusResourceImpl.class);

	private final ExternalAppManager pm;
	private final ErrorBuilder errorBuilder;
	private final CyApplicationManager appManager;

	private AppStatusResponse<AppStatusParameters> status;

	public NdexStatusResourceImpl(final ExternalAppManager pm, ErrorBuilder errorBuilder,
			CyApplicationManager appManager) {
		this.pm = pm;
		this.errorBuilder = errorBuilder;
		this.appManager = appManager;
	}

	private AppStatusParameters setSaveProps() {
		final CyNetwork network = appManager.getCurrentNetwork();

		if (network == null) {
			final String message = "Current network does not exist (No network is selected)";
			logger.error(message);
			throw errorBuilder.buildException(Status.BAD_REQUEST, message, ErrorType.INTERNAL);
		}

		final CyRootNetwork root = ((CySubNetwork) network).getRootNetwork();
		return buildProps(root);
	}

	private final AppStatusParameters buildProps(final CyRootNetwork root) {
		final SaveParameters summary = new SaveParameters();
		final CyTable table = root.getDefaultNetworkTable();

		// Network local table
		final CyRow row = table.getRow(root.getSUID());
		summary.suid = root.getSUID();
		summary.uuid = row.get(NdexStatusResource.NDEX_UUID_TAG, String.class);
		summary.name = row.get(CyNetwork.NAME, String.class);
		summary.author = row.get("author", String.class);
		summary.description = row.get("description", String.class);
		summary.disease = row.get("disease", String.class);
		summary.organism = row.get("organism", String.class);
		summary.reference = row.get("reference", String.class);
		summary.rights = row.get("rights", String.class);
		summary.rightsHolder = row.get("rightsHolder", String.class);
		summary.tissue = row.get("tissue", String.class);

		return summary;
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
		status.widget = widget;

		if (widget.equals(ExternalAppManager.APP_NAME_LOAD)) {
			status.parameters = setLoadProps();
		} else {
			status.parameters = setSaveProps();
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
