package org.cytoscape.cyndex2.internal.task;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.ToolTipManager;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.search.NetworkSearchTaskFactory;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskObserver;

public class OpenBrowseTaskFactory extends OpenDialogTaskFactory implements NetworkSearchTaskFactory {

	private static final String ID = "cyndex2";
	private static final String NAME = "NDEx - Network Search";
	private final Icon icon;

	private static Entry entry;

	public OpenBrowseTaskFactory(final Icon icon) {
		super(ExternalAppManager.APP_NAME_LOAD);
		this.icon = icon;
	}

	public static Entry getEntry() {
		if (entry == null)
			entry = new Entry();
		return entry;
	}

	@Override
	public String getName() {
		return NAME;
	}

	public static class Entry extends JTextField {
		/**
		 * 
		 */
		private static final long serialVersionUID = -5305178656253939245L;
		private final Font SEARCH_TEXT_FONT = new Font("SansSerif", Font.PLAIN, 12);
		private final Color TEXT_COLOR = Color.decode("#444444");
		private final static String SEARCH_TEXT = "Enter search terms for NDEx...";
		private boolean disabled = false;
		private JToolTip tip;

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

		public void setDisabled(String message) {
			setEnabled(false);
			setText("Unable to start CyNDEx2");
			tip = new JToolTip();
			tip.add(new JLabel(message));
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
			final Dimension size = new Dimension(220, 280);
			final JEditorPane pane = new JEditorPane();
			pane.setBackground(Color.white);
			pane.setEditable(false);
			pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
			pane.setFont(SEARCH_TEXT_FONT);
			pane.setForeground(TEXT_COLOR);
			pane.setContentType("text/html");
			final String help = "<h3>NDEx Database Search</h3>"
					+ "<p>Enter search query for NDEx database. You can use</p>"
					+ "<br/>  - Gene names<br/>  - Gene IDs<br/>  - Keywords<br/>  - Shared Network URL<br/>  - etc.<br/>"
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

	@Override
	public JComponent getQueryComponent() {
		return getEntry();
	}

	public String getQuery() {
		return getEntry().getQuery();
	}

	@Override
	public TaskIterator createTaskIterator() {
		ExternalAppManager.query = getQuery();
		return super.createTaskIterator();
	}

	@Override
	public String getDescription() {
		return "<html>The Network Data Exchange (NDEx) is a cloud-based database <br />"
				+ "and software infrastructure to store, share and publish <br />"
				+ "biological network knowledge. NDEx provides a REST API and<br />"
				+ "several client libraries are available for programmatic <br />"
				+ "access. NDEx users can request DOIs for their networks and <br />"
				+ "rely on full integration with Cytoscape. <br />"
				+ "NDEx is also available as an installation bundle for users <br />"
				+ "working with networks in a confidential environment. <br />"
				+ "For more information on NDEx and its Advanced Search <br />"
				+ "capabilities, please visit our website.</html>";
	}

	@Override
	public URL getWebsite() {
		try {
			return new URL("http://www.home.ndexbio.org/finding-and-querying-networks/#searchexamples");
		} catch (@SuppressWarnings("unused") MalformedURLException e) {
			return null;
		}
	}

	@Override
	public Icon getIcon() {
		return icon;
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public JComponent getOptionsComponent() {
		return null;
	}

	@Override
	public TaskObserver getTaskObserver() {
		return null;
	}

}
