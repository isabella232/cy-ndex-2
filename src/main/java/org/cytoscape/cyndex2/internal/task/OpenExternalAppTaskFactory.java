package org.cytoscape.cyndex2.internal.task;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.search.AbstractNetworkSearchTaskFactory;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.TaskIterator;

import com.teamdev.jxbrowser.chromium.swing.BrowserView;

public class OpenExternalAppTaskFactory extends AbstractNetworkSearchTaskFactory {

	private static final String ID = "netsearchtest.test-c";
	private static final String NAME = "CyNDEX-2";

	private final String appName;
	private final ExternalAppManager pm;
	private final CyEventHelper eventHelper;
	private final BrowserView browserView;
	private final JPanel panel;

	private final CyApplicationManager appManager;

	public OpenExternalAppTaskFactory(final String appName, final ExternalAppManager pm,
			final CyEventHelper eventHelper, final CyApplicationManager appManager, final BrowserView browserView,
			Icon icon, JPanel panel) {
		super(ID, NAME, icon);
		this.pm = pm;
		this.appName = appName;
		this.eventHelper = eventHelper;
		this.appManager = appManager;
		this.browserView = browserView;
		this.panel = panel;
	}

	
	public JComponent getQueryComponent() {
		if (browserView == null){
			JLabel label = new JLabel("Restart Cytoscape to use CyNdex");
			label.setForeground(Color.GRAY);
			label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
			return label;
		}
		if (panel.getComponents().length == 0)
			return super.getQueryComponent();
		return panel;
	}

	@Override
	public TaskIterator createTaskIterator() {
		pm.setQuery(getQuery());
		return new TaskIterator(new OpenExternalAppTask(appName, pm, eventHelper, browserView));
	}

	@Override
	public boolean isReady() {
		if (browserView == null)
			return false;
		if (appName == ExternalAppManager.APP_NAME_SAVE) {
			final CyNetwork curNetwork = appManager.getCurrentNetwork();
			if (curNetwork == null) {
				return false;
			} else {
				return true;
			}
		} else {
			return true;
		}
	}
}
