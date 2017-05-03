package org.cytoscape.hybrid.internal.rest.errors;

import java.net.URI;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.cytoscape.ci.CIErrorFactory;
import org.cytoscape.ci.model.CIError;
import org.cytoscape.hybrid.internal.rest.NdexResponse;

public class ErrorBuilder {

	private final CIErrorFactory ciErrorFactory;

	public ErrorBuilder(final CIErrorFactory ciErrorFactory) {
		this.ciErrorFactory = ciErrorFactory;
	}

	public Response buildErrorResponse(final Status status, final String message) {

		final CIError ciError = ciErrorFactory.getCIError(Status.BAD_REQUEST.getStatusCode(),
				"urn:cytoscape:ci:ndex:v1:errors:1", message, URI.create("file:///"));
		NdexResponse<Object> ndexResponse = new NdexResponse<>();
		ndexResponse.getErrors().add(ciError);

		final Response res = Response.status(status).type(MediaType.APPLICATION_JSON).entity(ndexResponse).build();

		return res;
	}

}
