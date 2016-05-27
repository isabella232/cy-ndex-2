package org.cytoscape.fx.internal.ui;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.events.CytoPanelStateChangedEvent;
import org.cytoscape.application.swing.events.CytoPanelStateChangedListener;
import org.cytoscape.fx.internal.task.HeadlessTaskMonitor;
import org.cytoscape.fx.internal.task.ShowPanelTaskFactory;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.TaskIterator;

public class PanelStatusManager implements CytoPanelStateChangedListener {

	private final ShowPanelTaskFactory tf;
	private final CySwingApplication app;
	private final CyServiceRegistrar reg;

	public PanelStatusManager(final ShowPanelTaskFactory tf, final CySwingApplication app,
			final CyServiceRegistrar reg) {
		this.tf = tf;
		this.app = app;
		this.reg = reg;
	}

	@Override
	public void handleEvent(CytoPanelStateChangedEvent evt) {

		final CytoPanel panel = evt.getCytoPanel();
		if (panel.getCytoPanelName() != CytoPanelName.WEST) {
			return;
		}

		// This is the WEST panel.
		// Find existing UI

//		final int tabCount = panel.getCytoPanelComponentCount();
//
//		for (int i = 0; i < tabCount; i++) {
//			final Component tab = panel.getComponentAt(i);
//			if (tab instanceof WebViewPanel) {
//				reg.unregisterAllServices(tab);
//				System.out.println("!!!!!!!!!!!! Panel removed: " + tab);
//			}
//		}

		//createPanel();
	}

	private final void createPanel() {
		System.out.println("\n\n\n Creatring new Panel \n\n");
		
		final TaskIterator itr = tf.createTaskIterator();
		try {
			itr.next().run(new HeadlessTaskMonitor());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
