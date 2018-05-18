package org.cytoscape.cyndex2.internal.rest.errors;

import java.io.File;
import java.net.URI;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.ci.CIErrorFactory;
import org.cytoscape.ci.CIExceptionFactory;
import org.cytoscape.ci.model.CIError;
import org.cytoscape.cyndex2.internal.util.CIServiceManager;

public class ErrorBuilder {

	private final CIServiceManager ciServiceManager;

	private final URI logFileUri;

	public ErrorBuilder(CIServiceManager ciServiceManager,
			final CyApplicationConfiguration appConfig) {

		this.ciServiceManager = ciServiceManager; 
		final File configDir = appConfig.getConfigurationDirectoryLocation();

		final File cy3Dir = new File(configDir, "3");
		final File logFile = new File(cy3Dir, "framework-cytoscape.log");

		this.logFileUri = logFile.toURI();
	}

	public CIError buildErrorResponse(final Status status, final String message, ErrorType type) {
		return ciServiceManager.getCIErrorFactory().getCIError(status.getStatusCode(), type.getUrn(), message, logFileUri);
	}

	public WebApplicationException buildException(final Status status, final String message, ErrorType type) {
		return ciServiceManager.getCIExceptionFactory().getCIException(status.getStatusCode(),
				new CIError[] { buildErrorResponse(status, message, type) });
	}
}
