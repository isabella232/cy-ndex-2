package org.cytoscape.fx.internal.ws.message;

public class InterAppMessage {
	
	public static final String FROM_CY3 = "cy3";
	public static final String FROM_NDEX = "ndex";
	
	public static final String TYPE_FOCUS = "focus";
	public static final String TYPE_QUERY = "query";
	public static final String TYPE_CLOSED = "closed";

	private String from;
	private String type;
	private String body;
	
	
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}

}
