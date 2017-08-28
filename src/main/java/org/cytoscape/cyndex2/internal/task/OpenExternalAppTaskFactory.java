package org.cytoscape.cyndex2.internal.task;

import java.util.Properties;

import javax.swing.Icon;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.search.AbstractNetworkSearchTaskFactory;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.property.CyProperty;
import org.cytoscape.work.TaskIterator;

import com.teamdev.jxbrowser.chromium.swing.BrowserView;

public class OpenExternalAppTaskFactory extends AbstractNetworkSearchTaskFactory {

	private static final String ID = "netsearchtest.test-c";
	private static final String NAME = "CyNDEX-2";

	private final String appName;
	private final ExternalAppManager pm;
	private final CyProperty<Properties> props;
	private final CyEventHelper eventHelper;
	private final BrowserView browserView;

	private final CyApplicationManager appManager;

	public OpenExternalAppTaskFactory(final String appName, final ExternalAppManager pm,
			final CyProperty<Properties> props, final CyEventHelper eventHelper, final CyApplicationManager appManager,
			final BrowserView browserView, Icon icon) {
		super(ID, NAME, icon);
		this.pm = pm;
		this.appName = appName;
		this.props = props;
		this.eventHelper = eventHelper;
		this.appManager = appManager;
		this.browserView = browserView;
	}

	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new OpenExternalAppTask(appName, pm, props, eventHelper, browserView));
	}

	@Override
	public boolean isReady() {

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
