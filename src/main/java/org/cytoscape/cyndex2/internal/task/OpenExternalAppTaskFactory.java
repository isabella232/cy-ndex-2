package org.cytoscape.cyndex2.internal.task;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.search.AbstractNetworkSearchTaskFactory;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

public class OpenExternalAppTaskFactory extends AbstractNetworkSearchTaskFactory implements TaskFactory {

	private static final String ID = "cyndex2";
	private static final String NAME = "CyNDEX-2";

	private final String appName;
	private final CyEventHelper eventHelper;
	private final ExternalAppManager pm;
	private final CyApplicationManager appManager;
	private JTextField entry;
	private JDialog dialog;

	public OpenExternalAppTaskFactory(final String appName, final CyEventHelper eventHelper,
			final CyApplicationManager appManager, final Icon icon, final ExternalAppManager pm, final JDialog dialog) {
		super(ID, NAME, icon);
		this.appName = appName;
		this.eventHelper = eventHelper;
		this.appManager = appManager;
		this.pm = pm;
		this.dialog = dialog;
	}

	public JTextField getEntry() {
		if (entry == null) {
			entry = new JTextField("Search CyNDex");
			entry.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			entry.setForeground(Color.GRAY);
			entry.addFocusListener(new FocusListener() {
				@Override
				public void focusGained(FocusEvent e) {
					if (entry.getForeground() == Color.GRAY) {
						entry.setText("");
						entry.setForeground(Color.BLACK);
					}
				}

				@Override
				public void focusLost(FocusEvent e) {
					if (entry.getText().isEmpty()) {
						entry.setForeground(Color.GRAY);
						entry.setText("Search CyNDex");
					}
				}
			});
		}
		return entry;
	}

	public JComponent getQueryComponent() {
		if (pm.loadFailed()) {
			JLabel label = new JLabel("Restart Cytoscape to use CyNDex");
			label.setForeground(Color.GRAY);
			label.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));
			return label;
		}
		return getEntry();
	}

	@Override
	public String getQuery() {
		JTextField entry = getEntry();
		return entry.getForeground() == Color.GRAY ? "" : entry.getText();
	}

	@Override
	public TaskIterator createTaskIterator() {
		pm.setQuery(getQuery());
		return new TaskIterator(new OpenExternalAppTask(appName, eventHelper, pm, dialog));
	}

	@Override
	public boolean isReady() {
		if (pm.loadFailed())
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
