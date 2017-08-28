package org.cytoscape.cyndex2.events;

import org.cytoscape.event.CyListener;

public interface ExternalAppClosedEventListener extends CyListener {

	public void handleEvent(final ExternalAppClosedEvent event);

}
