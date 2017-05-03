package org.cytoscape.hybrid.internal.rest;

import java.util.ArrayList;
import java.util.List;

import org.cytoscape.ci.model.CIError;

public final class NdexResponse<T> {

	// Errors reported from the called service(s)
	private List<CIError> errors;

	// Actual result for the API call
	private T data;
	
	public NdexResponse() {
		this.errors = new ArrayList<>();
	}

	public List<CIError> getErrors() {
		return errors;
	}

	public void setErrors(final List<CIError> errors) {
		this.errors = errors;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}
}
