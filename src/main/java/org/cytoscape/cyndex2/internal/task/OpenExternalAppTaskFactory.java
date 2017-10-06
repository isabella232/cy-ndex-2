package org.cytoscape.cyndex2.internal.task;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Dialog.ModalityType;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
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
	private static final String NAME = "CyNDEx-2";

	private final String appName;
	private final ExternalAppManager pm;
	private final CyApplicationManager appManager;
	private static Entry entry;
	private String port;
	private static boolean loadFailed = false;
	private static JDialog dialog;

	public OpenExternalAppTaskFactory(final String appName, final CyApplicationManager appManager, final Icon icon,
			final ExternalAppManager pm, final CySwingApplication swingApp, final CyProperty<Properties> cyProps) {
		super(ID, NAME, icon);
		this.appName = appName;
		this.appManager = appManager;
		this.pm = pm;
		port = cyProps.getProperties().getProperty("rest.port");
		entry = new Entry();

		if (port == null)
			port = "1234";

		JFrame frame = swingApp.getJFrame();
		dialog = new JDialog(frame, "CyNDEx2 Browser", ModalityType.APPLICATION_MODAL);
		// ensure modality type
		dialog.getModalityType();

	}

	public static boolean loadFailed() {
		return loadFailed;
	}

	public static void setLoadFailed(String reason) {
		entry.setDisabled();
		//entry.setToolTipText(reason);
		loadFailed = true;

	}

	public static void cleanup() {
		if (dialog != null) {
			dialog.setVisible(false);
			dialog.dispose();
		}
	}

	private class Entry extends JTextField {
		/**
		 * 
		 */
		private static final long serialVersionUID = -5305178656253939245L;
		private final Font SEARCH_TEXT_FONT = new Font("SansSerif", Font.PLAIN, 12);
		private final Color TEXT_COLOR = Color.decode("#444444");
		private final static String SEARCH_TEXT = "Enter search terms for NDEx...";
		private boolean disabled = false;
		private final JToolTip tip;

		public Entry() {
			super(SEARCH_TEXT);
			setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			setForeground(Color.GRAY);
			setToolTipText("");
			tip = new JToolTip();

			addKeyListener(new KeyAdapter() {
				@Override
				public void keyTyped(KeyEvent e) {
					super.keyTyped(e);
				}

				@Override
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_ENTER) {
						fireSearchRequested();
					} else {
						firePropertyChange(QUERY_PROPERTY, null, null);
					}
					super.keyPressed(e);
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

		public void setDisabled() {
			setEnabled(false);
			setText("Restart Cytoscape to use CyNDEx2");
			setForeground(Color.GRAY);
			disabled = true;
			firePropertyChange(QUERY_PROPERTY, null, null);
		}

		@Override
		public JToolTip createToolTip() {
			ToolTipManager.sharedInstance().setDismissDelay(7000);
			if (disabled) {
				tip.setComponent(this);
				tip.setLayout(new BorderLayout());
				tip.add(new JLabel("Likely caused by a failure to update CyNDEx2 or an "
						+ "issue with existing JXBrowser instances."), BorderLayout.CENTER);
				return tip;
			}
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

			tip.setPreferredSize(size);
			tip.setSize(size);
			tip.setComponent(this);
			tip.setLayout(new BorderLayout());
			tip.add(pane, BorderLayout.CENTER);
			tip.setVisible(true);
			return tip;
		}

		private void fireSearchRequested() {
			firePropertyChange(SEARCH_REQUESTED_PROPERTY, null, null);
		}

	}

	public JComponent getQueryComponent() {
		return entry;
	}

	@Override
	public String getQuery() {
		return entry.getQuery();
	}

	@Override
	public TaskIterator createTaskIterator() {
		// Store query info
		pm.setQuery(getQuery());
		pm.setAppName(appName);
		pm.setPort(port);

		dialog.setSize(1000, 700);
		dialog.setLocationRelativeTo(null);

		TaskIterator ti = new TaskIterator();
		LoadBrowserTask loader = new LoadBrowserTask(pm, dialog, ti);
		ti.append(loader);
		return ti;
	}

	@Override
	public boolean isReady() {
		if (loadFailed)
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
