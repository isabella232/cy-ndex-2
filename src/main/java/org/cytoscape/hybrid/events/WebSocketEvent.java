package org.cytoscape.hybrid.events;

import org.cytoscape.event.AbstractCyEvent;

public final class WebSocketEvent extends AbstractCyEvent<Object> {

	private final InterAppMessage message;

	public WebSocketEvent(final Object source, final InterAppMessage message) {
		super(source, WebSocketEventListener.class);
		this.message = message;
	}

	public final InterAppMessage getMessage() {
		return this.message;
	}
}
