package org.cytoscape.hybrid.events;

import org.cytoscape.event.AbstractCyEvent;

public final class ExternalAppClosedEvent extends AbstractCyEvent<Object> {
	
	public ExternalAppClosedEvent(final Object source) {
		super(source, ExternalAppClosedEventListener.class);
	}
}
