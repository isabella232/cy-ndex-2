package org.cytoscape.cyndex2.internal.task;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.ToolTipManager;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
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
	private final CySwingApplication swingApp;
	private String port;

	public OpenExternalAppTaskFactory(final String appName, final CyApplicationManager appManager, final Icon icon,
			final ExternalAppManager pm, final CySwingApplication swingApp, final CyProperty<Properties> cyProps) {
		super(ID, NAME, icon);
		this.appName = appName;
		this.appManager = appManager;
		this.pm = pm;
		this.swingApp = swingApp;
		port = cyProps.getProperties().getProperty("rest.port");
		if (port == null)
			port = "1234";
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
		private final Font SEARCH_TEXT_FONT = new Font("SansSerif", Font.PLAIN, 12);
		private final Color TEXT_COLOR  = Color.decode("#444444");
		private final static String SEARCH_TEXT = "Enter search terms for NDEx...";
		
		public Entry() {
			super(SEARCH_TEXT);
			setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			setForeground(Color.GRAY);
			setToolTipText("TOOLTIP");
			
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
						setText(SEARCH_TEXT);
					}
				}
			});

		}

		public String getQuery() {
			return getForeground() == Color.GRAY ? "" : getText();
		}
		
		@Override
		public JToolTip createToolTip() {
			final Dimension size = new Dimension(220, 270);
			final JEditorPane pane = new JEditorPane();
			pane.setBackground(Color.white);
			pane.setEditable(false);
			pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
			pane.setFont(SEARCH_TEXT_FONT);
			pane.setForeground(TEXT_COLOR);
			pane.setContentType("text/html");
			final String help = "<h3>NDEx Database Search</h3>"
					+ "<p>Enter search query for NDEx database. You can use</p>"
					+ "<br/>  - Gene names<br/>  - Gene IDs<br/>  - Keywords<br/>  - etc.<br/>"
					+ "<p>If you want to browse the database, simply send empty query. For more details, please visit <i>www.ndexbio.org</i></p>";
			pane.setText(help);
			pane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
			JToolTip tip = super.createToolTip();
			ToolTipManager.sharedInstance().setDismissDelay(10000);
			
			tip.setPreferredSize(size);
			tip.setSize(size);
			tip.setComponent(this);
			tip.setLayout(new BorderLayout());
			tip.add(pane, BorderLayout.CENTER);
			return tip;
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
		return new TaskIterator(new OpenExternalAppTask(appName, pm, swingApp, port));
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
