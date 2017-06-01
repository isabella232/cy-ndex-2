package org.cytoscape.hybrid.events;

import org.cytoscape.event.AbstractCyEvent;

public class ExternalAppStartedEvent extends AbstractCyEvent<Object> {

	public ExternalAppStartedEvent(final Object source) {
		super(source, ExternalAppStartedEventListener.class);
	}
}
