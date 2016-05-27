package org.cytoscape.fx.internal.ws.message;

public class InterAppMessage {
	
	public static final String CY3 = "cy3";
	public static final String NDEX = "ndex";

	private String type;
	private String message;

	public void setType(String type) {
		this.type = type;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getType() {
		return this.type;
	}

	public String getMessage() {
		return this.message;
	}

}
