package org.cytoscape.cyndex2.internal.task;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.ToolTipManager;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.search.AbstractNetworkSearchTaskFactory;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.property.CyProperty;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.ndexbio.model.exceptions.NdexException;

public class OpenBrowseTaskFactory extends AbstractNetworkSearchTaskFactory {

	private static final String ID = "cyndex2";
	private static final String NAME = "NDEx Database";

	private final String appName;
	private final ExternalAppManager pm;
	private static Entry entry;
	private String port;
	private final JDialog dialog;

	public OpenBrowseTaskFactory(final CyApplicationManager appManager, final Icon icon, final ExternalAppManager pm,
			final CySwingApplication swingApp, final CyProperty<Properties> cyProps) {
		super(ID, NAME, icon);
		this.appName = ExternalAppManager.APP_NAME_LOAD;
		this.pm = pm;
		port = cyProps.getProperties().getProperty("rest.port");

		if (port == null)
			port = "1234";

		JFrame frame = swingApp.getJFrame();
		dialog = pm.getDialog(frame);
	}

	public static Entry getEntry() {
		if (entry == null)
			entry = new Entry();
		return entry;
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

	public JComponent getQueryComponent() {
		return getEntry();
	}

	@Override
	public String getQuery() {
		return getEntry().getQuery();
	}

	@Override
	public TaskIterator createTaskIterator() {
		TaskIterator ti = new TaskIterator();

		if (ExternalAppManager.busy)
			return ti;
		String query = getQuery();
		try {
			
			URL url = new URL(query);
			assert url.getHost().contains("ndexbio.org");
			
			Pattern pattern = Pattern
					.compile("^(https?://www\\.[^#]*ndexbio.org/)#/network/([^\\?]*)(\\?accesskey=(.*))?$", Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(query);
			String hostName = null, uuid = null, accessKey = null;
			if (matcher.find()) {
				hostName = matcher.group(1);
				uuid = matcher.group(2);
				accessKey = matcher.group(4);
			}
			hostName += "v2/";
			System.out.println(hostName + " " + uuid + " " + accessKey);
			if (hostName != null && uuid != null){
				NetworkImportTask importer = new NetworkImportTask(hostName, UUID.fromString(uuid));
				if (accessKey != null)
					importer.setAccessKey(accessKey);
				ti.append(importer);
				return ti;
			}
		} catch (MalformedURLException e) {
			// NOT a url
		} catch (IOException e){
			
		} catch(NdexException e){
			ti.append(new Task() {
				
				@Override
				public void run(TaskMonitor arg0) throws Exception {
					JOptionPane.showMessageDialog(null, e.getMessage());
					
				}
				
				@Override
				public void cancel() {
					// TODO Auto-generated method stub
					
				}
			});
		}

		// Store query info
		pm.setQuery(getQuery());
		pm.setAppName(appName);
		pm.setPort(port);
		ExternalAppManager.busy = true;

		dialog.setSize(1000, 700);
		dialog.setLocationRelativeTo(null);

		LoadBrowserTask loader = new LoadBrowserTask(pm, dialog, ti);
		ti.append(loader);

		return ti;
	}

	@Override
	public boolean isReady() {
		return !ExternalAppManager.busy && !ExternalAppManager.loadFailed;
	}

}
