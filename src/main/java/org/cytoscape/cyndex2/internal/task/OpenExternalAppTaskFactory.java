package org.cytoscape.cyndex2.internal.task;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.search.AbstractNetworkSearchTaskFactory;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.property.CyProperty;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

public class OpenExternalAppTaskFactory extends AbstractNetworkSearchTaskFactory implements TaskFactory {

	private static final String ID = "cyndex2";
	private static final String NAME = "CyNDEX-2";

	private final String appName;
	private final ExternalAppManager pm;
	private final CyApplicationManager appManager;
	private Entry entry;
	private final JDialog dialog;
	private final String port;

	public OpenExternalAppTaskFactory(final String appName, final CyApplicationManager appManager, final Icon icon,
			final ExternalAppManager pm, final JDialog dialog, final CyProperty<Properties> cyProps) {
		super(ID, NAME, icon);
		this.appName = appName;
		this.appManager = appManager;
		this.pm = pm;
		this.dialog = dialog;
		port = cyProps.getProperties().getProperty("rest.port");
	}

	public Entry getEntry() {
		if (entry == null) {
			entry = new Entry();
		}
		return entry;
	}

	private class Entry extends JTextField {
		/**
		 * 
		 */
		private static final long serialVersionUID = -5305178656253939245L;

		public Entry() {
			super("Search CyNDex");
			setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			setForeground(Color.GRAY);
			addKeyListener(new KeyAdapter() {
				@Override
				public void keyTyped(KeyEvent e) {
					super.keyTyped(e);
				}

				@Override
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_ENTER)
						fireSearchRequested();
					else {
						super.keyPressed(e);
						firePropertyChange(QUERY_PROPERTY, null, null);
					}
				}

			});

			addFocusListener(new FocusListener() {
				@Override
				public void focusGained(FocusEvent e) {
					if (getForeground() == Color.GRAY) {
						setText("");
						setForeground(Color.BLACK);
					}
				}

				@Override
				public void focusLost(FocusEvent e) {
					if (getText().isEmpty()) {
						setForeground(Color.GRAY);
						setText("Search CyNDex");
					}
				}
			});

		}

		public String getQuery() {
			return getForeground() == Color.GRAY ? "" : getText();
		}

		private void fireSearchRequested() {
			firePropertyChange(SEARCH_REQUESTED_PROPERTY, null, null);
		}
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
		Entry entry = getEntry();
		return entry.getQuery();
	}

	@Override
	public TaskIterator createTaskIterator() {
		pm.setQuery(getQuery());
		return new TaskIterator(new OpenExternalAppTask(appName, pm, dialog, port));
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
