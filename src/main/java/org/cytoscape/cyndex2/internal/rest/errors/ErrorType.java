package org.cytoscape.cyndex2.internal.rest.errors;

public enum ErrorType {
	
	AUTH_FAILED("Login to NDEx failed."),
	NDEX_API("Failed to call NDEx API."),
	INVALID_PARAMETERS("Invalid parameters are given."),
	INTERNAL("Internal Cytoscape function calls failed.");
	
	private static final String BASE_URN = "urn:cytoscape:ci:ndex:v1:errors:";
	
	private final String urn;
	private final String description;
	
	private ErrorType(final String description) {
		this.description = description;
		this.urn = BASE_URN + this.ordinal();
	}
	
	public String getUrn() {
		return urn;
	}
	
	public String getDescription() {
		return description;
	}
	
	
	
	
}
