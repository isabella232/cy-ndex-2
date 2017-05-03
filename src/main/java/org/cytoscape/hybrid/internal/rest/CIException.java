package org.cytoscape.hybrid.internal.rest;

import javax.ws.rs.WebApplicationException;

public class CIException extends WebApplicationException {

	private final CIError error;

	public CIException(String message, Throwable throwable, CIError error) {
		super(message, throwable);
		this.error = error;
	}

	public CIError getError() {
		return this.error;
	}
}
