package org.cytoscape.hybrid.events;

import org.cytoscape.event.CyListener;

public interface ExternalAppClosedEventListener extends CyListener {

	public void handleEvent(final ExternalAppClosedEvent event);

}
