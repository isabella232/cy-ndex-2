package org.cytoscape.cyndex2.internal.rest.errors;

import java.io.File;
import java.net.URI;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.ci.CIErrorFactory;
import org.cytoscape.ci.CIExceptionFactory;
import org.cytoscape.ci.model.CIError;

public class ErrorBuilder {

	private final CIErrorFactory ciErrorFactory;
	private final CIExceptionFactory ciExceptionFactory;

	private final URI logFileUri;

	public ErrorBuilder(final CIErrorFactory ciErrorFactory, final CIExceptionFactory ciExceptionFactory,
			final CyApplicationConfiguration appConfig) {
		this.ciErrorFactory = ciErrorFactory;
		this.ciExceptionFactory = ciExceptionFactory;

		final File configDir = appConfig.getConfigurationDirectoryLocation();

		final File cy3Dir = new File(configDir, "3");
		final File logFile = new File(cy3Dir, "framework-cytoscape.log");

		this.logFileUri = logFile.toURI();
	}

	public CIError buildErrorResponse(final Status status, final String message, ErrorType type) {
		return ciErrorFactory.getCIError(status.getStatusCode(), type.getUrn(), message, logFileUri);
	}

	public WebApplicationException buildException(final Status status, final String message, ErrorType type) {
		return ciExceptionFactory.getCIException(status.getStatusCode(),
				new CIError[] { buildErrorResponse(status, message, type) });
	}
}
