package org.cytoscape.hybrid.internal.rest;

public abstract class NdexResponse {

	private CiError error;
	
	public CiError getError() {
		return error;
	}
	
	public void setError(CiError error) {
		this.error = error;
	}
	
}
