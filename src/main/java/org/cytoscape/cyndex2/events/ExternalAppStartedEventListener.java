package org.cytoscape.cyndex2.events;

import org.cytoscape.event.CyListener;

public interface ExternalAppStartedEventListener extends CyListener {

	public void handleEvent(final ExternalAppStartedEvent event);

}
