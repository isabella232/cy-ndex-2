package org.cytoscape.hybrid.internal.rest.errors;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.cytoscape.hybrid.internal.rest.CIError;
import org.cytoscape.hybrid.internal.rest.NdexResponse;

public class ErrorBuilder {
	
	
	public static Response buildErrorResponse(final Status status, final String message) {
		
			final CIError error = new CIError(
					status.getStatusCode(),
					"urn:cytoscape:ci:ndex:v1:networks:errors:2",
					message, "/log");
			NdexResponse<Object> ndexResponse = new NdexResponse<>();
			ndexResponse.getErrors().add(error);
			
			final Response res = Response
					.status(status)
					.type(MediaType.APPLICATION_JSON)
					.entity(ndexResponse).build();
		
			return res;
	}

}
