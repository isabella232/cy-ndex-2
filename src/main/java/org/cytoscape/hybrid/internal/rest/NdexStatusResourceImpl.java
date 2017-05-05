package org.cytoscape.hybrid.internal.rest;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.core.Response.Status;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.ci.CIWrapping;
import org.cytoscape.hybrid.internal.rest.errors.ErrorBuilder;
import org.cytoscape.hybrid.internal.rest.errors.ErrorType;
import org.cytoscape.hybrid.internal.ws.ExternalAppManager;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NdexStatusResourceImpl implements NdexStatusResource {
	
	private static final Logger logger = LoggerFactory.getLogger(NdexStatusResourceImpl.class);

	private static final String NDEX_PREFIX = "ndex.";
	
	private final ExternalAppManager pm;
	private final ErrorBuilder errorBuilder;
	private final CyApplicationManager appManager;
	
	private Map<String, Object> status;
	private Object parameters;


	public NdexStatusResourceImpl(final ExternalAppManager pm, ErrorBuilder errorBuilder, CyApplicationManager appManager) {
		this.pm = pm;
		this.errorBuilder = errorBuilder;
		this.appManager = appManager;
	}

	private void setSaveProps() {
		final CyNetwork network = appManager.getCurrentNetwork();
		
		if (network == null) {
			final String message = "Current network does not exist (No network is selected)";
			logger.error(message);
			throw errorBuilder.buildException(Status.BAD_REQUEST, message, ErrorType.INTERNAL);
		}
		
		final CyRootNetwork root = ((CySubNetwork) network).getRootNetwork();
		parameters = buildProps(root);

	}
	
	private final SaveProps buildProps(final CyRootNetwork root) {
		final SaveProps summary = new SaveProps();
		final CyTable table = root.getDefaultNetworkTable();

		// Network local table
		final CyRow row = table.getRow(root.getSUID());
		summary.suid = root.getSUID();
		summary.uuid = row.get("uuid", String.class);
		summary.name = row.get(CyNetwork.NAME, String.class);
		summary.author = row.get("author", String.class);
		summary.descriptions = row.get("descriptions", String.class);
		summary.disease = row.get("disease", String.class);
		summary.organism = row.get("organism", String.class);
		summary.reference = row.get("reference", String.class);
		summary.rights = row.get("rights", String.class);
		summary.rightsHolder = row.get("rightsHoulder", String.class);
		summary.tissue = row.get("tissue", String.class);

		return summary;
	}

	private void setLoadProps() {
		parameters = new TreeMap<String, Object>();
		((Map<String, Object>)parameters).put("searchTerm", pm.getQuery());
	}

	@Override
	@CIWrapping
	public Map<String, Object> getAppStatus() {

		final String widget = pm.getAppName();
		if(widget == null) {
			final String message = "Application type is not set yet.";
			logger.error(message);
			throw errorBuilder.buildException(Status.INTERNAL_SERVER_ERROR, message, ErrorType.INTERNAL);
		}

		status = new TreeMap<>();

		status.put("widget", widget);

		if (widget.equals(ExternalAppManager.APP_NAME_LOAD)) {
			setLoadProps();
		} else {
			setSaveProps();
		}

		status.put("parameters", parameters);
		return status;
	}
}
