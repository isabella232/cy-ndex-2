package org.cytoscape.hybrid.events;


/**
 * 
 * Message exchanged between two applications: Cytoscape and Electron
 * 
 */
public class InterAppMessage {
	
	// Source of message
	public static final String FROM_CY3 = "cy3";
	public static final String FROM_NDEX = "ndex";
	
	// Type of messages
	public static final String TYPE_FOCUS = "focus";
	public static final String TYPE_FOCUS_SUCCESS = "focus-success";
	public static final String TYPE_FOCUS_LOST = "focus-lost";
	public static final String TYPE_QUERY = "query";
	public static final String TYPE_CLOSED = "closed";
	public static final String TYPE_CONNECTED = "connected";
	public static final String TYPE_ALIVE = "alive";
	public static final String TYPE_APP = "app";
	public static final String TYPE_MINIMIZED = "minimized";
	public static final String TYPE_RESTORED = "restored";

	private String from;
	private String type;
	private String body;
	private Object options;
	
	// Static factory
	public static final InterAppMessage create() {
		final InterAppMessage msg = new InterAppMessage();
		return msg;
	}
	
	public String getFrom() {
		return from;
	}
	public InterAppMessage setFrom(String from) {
		this.from = from;
		return this;
	}
	public String getType() {
		return type;
	}
	public InterAppMessage setType(String type) {
		this.type = type;
		return this;
	}
	public String getBody() {
		return body;
	}
	public InterAppMessage setBody(String body) {
		this.body = body;
		return this;
	}

	public Object getOptions() {
		return options;
	}

	public InterAppMessage setOptions(Object options) {
		this.options = options;
		return this;
	}
}