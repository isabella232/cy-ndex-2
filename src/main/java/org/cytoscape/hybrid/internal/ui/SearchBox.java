package org.cytoscape.hybrid.internal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.ToolTipManager;
import javax.swing.border.EmptyBorder;

import org.cytoscape.hybrid.events.ExternalAppClosedEvent;
import org.cytoscape.hybrid.events.ExternalAppClosedEventListener;
import org.cytoscape.hybrid.events.ExternalAppStartedEvent;
import org.cytoscape.hybrid.events.ExternalAppStartedEventListener;
import org.cytoscape.hybrid.internal.task.OpenExternalAppTaskFactory;
import org.cytoscape.hybrid.internal.ws.ExternalAppManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchBox extends JPanel implements ExternalAppClosedEventListener, ExternalAppStartedEventListener {

	private static final long serialVersionUID = 5216512744558942600L;

	private static final Logger LOGGER = LoggerFactory.getLogger(SearchBox.class);
	
	private static final Icon NDEX_LOGO = new ImageIcon(
			SearchBox.class.getClassLoader().getResource("images/ndex-logo.png"));

	private static final Icon ICON_SEARCH = new ImageIcon(
			SearchBox.class.getClassLoader().getResource("images/search.png"));
	private static final Icon ICON_SETTINGS = new ImageIcon(
			SearchBox.class.getClassLoader().getResource("images/settings.png"));

	private static final Dimension PANEL_SIZE = new Dimension(400, 40);
	private static final Dimension PANEL_SIZE_MAX = new Dimension(900, 100);
	private static final Dimension TEXT_AREA_SIZE = new Dimension(250, 24);

	private static final Dimension BUTTON_SIZE = new Dimension(30, 30);
	
	private static final Font SEARCH_TEXT_FONT = new Font("SansSerif", Font.BOLD, 11);
	private static final Font SEARCH_TEXT_FONT2 = new Font("SansSerif", Font.PLAIN, 12);
	private static final Color TEXT_COLOR  = Color.decode("#444444");

	private static final String PLACEHOLDER = "Enter search terms for NDEx...";

	private final JLabel iconLabel;
	private final JTextField searchTextField;
	private final JPanel searchButton;

	// States
	private Boolean searchBoxClicked = false;

	// For child process management
	private final ExternalAppManager pm;

	private final OpenExternalAppTaskFactory tf;
	private final TaskManager<?, ?> tm;

	
	public SearchBox(final ExternalAppManager pm, 
			OpenExternalAppTaskFactory tf, TaskManager<?, ?> tm) {

		this.pm = pm;
		this.tf = tf;
		this.tm = tm;
		
		this.setPreferredSize(PANEL_SIZE);
		this.setSize(PANEL_SIZE);
		this.setMaximumSize(PANEL_SIZE_MAX);

		this.iconLabel = new JLabel();
		this.iconLabel.setBorder(new EmptyBorder(3,5,3,5));
		this.iconLabel.setBackground(Color.WHITE);
		this.iconLabel.setSize(BUTTON_SIZE);
		this.iconLabel.setIcon(NDEX_LOGO);

		this.searchTextField = new JTextField() {
			
			private static final long serialVersionUID = -8641366271435349606L;

			@Override
			public JToolTip createToolTip() {
				final Dimension size = new Dimension(220, 270);
				final JEditorPane pane = new JEditorPane();
				pane.setPreferredSize(size);
				pane.setBackground(Color.white);
				pane.setEditable(false);
				pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
				pane.setFont(SEARCH_TEXT_FONT2);
				pane.setForeground(TEXT_COLOR);
				pane.setContentType("text/html");
				final String help = "<h3>NDEx Database Search</h3>"
						+ "<p>Enter search query for NDEx database. You can use</p>"
						+ "<br/>  - Gene names<br/>  - Gene IDs<br/>  - Keywords<br/>  - etc.<br/>"
						+ "<p>If you want to browse the database, simply send empty query. For more details, please visit <i>www.ndexbio.org</i></p>";
				pane.setText(help);
				pane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

				ToolTipManager.sharedInstance().setDismissDelay(10000);
				JToolTip tip = new JToolTip();
				tip.setPreferredSize(size);
				tip.setSize(size);
				tip.setComponent(this);
				tip.setLayout(new BorderLayout());
				tip.add(pane, BorderLayout.CENTER);
				return tip;
			}
		};
		
		this.searchTextField.setToolTipText("");
	
		this.searchTextField.setBorder(null);
		this.searchTextField.setFont(SEARCH_TEXT_FONT);
		this.searchTextField.setBackground(Color.white);
		this.searchTextField.setForeground(Color.decode("#333333"));
		this.searchTextField.setPreferredSize(TEXT_AREA_SIZE);
		this.searchTextField.setSize(TEXT_AREA_SIZE);
		this.searchTextField.setText(PLACEHOLDER);
		
		this.searchTextField.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				processClick();
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
				if (!searchBoxClicked) {
					searchTextField.setText("");
					searchBoxClicked = true;
				}
			}
			
			private final void processClick() {

				if (!searchBoxClicked) {
					searchTextField.setText("");
					searchBoxClicked = true;
				}
			}
		});
			
		
		this.searchTextField.addActionListener(e -> {
			try {
				search(searchTextField.getText());
			} catch (Exception ex) {
				ex.printStackTrace();
				LOGGER.error("Could not finish search.", ex);
			}
		});		

		this.searchButton = new JPanel();
		this.searchButton.setLayout(new BorderLayout());
		this.searchButton.setBorder(new EmptyBorder(3, 5, 3, 5));
		this.searchButton.setBackground(Color.WHITE);
		
		final JLabel searchIconLabel = new JLabel(ICON_SEARCH);
		final JLabel settingIconLabel = new JLabel(ICON_SETTINGS);
		searchIconLabel.setOpaque(false);
		settingIconLabel.setOpaque(false);
		this.searchButton.add(searchIconLabel, BorderLayout.CENTER);
		this.searchButton.setSize(BUTTON_SIZE);
		this.searchButton.setBackground(Color.white);
		this.searchButton.setToolTipText("Start NDEx search (opens new window)");
		this.searchButton.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent e) {
				
				// Ignore if already open.
				if(!searchButton.isEnabled()) {
					return;
				}
				
				try {
					search(searchTextField.getText());
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});

		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(1, 2));
		buttonPanel.add(searchButton);

		setLayout(new BorderLayout());
		add(iconLabel, BorderLayout.WEST);
		add(searchTextField, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.EAST);
		
		this.setOpaque(true);
		this.setBackground(Color.WHITE);
		this.setBorder(new EmptyBorder(3,3,3,3));
	}

	
	private final void search(String query) throws Exception {
		// Use empty String if default text is used.
		if (query.equals(PLACEHOLDER)) {
			query = "";
		}

		pm.setQuery(query);
		
		final TaskIterator itr = tf.createTaskIterator();
		tm.execute(itr);
	}


	@Override
	public void handleEvent(ExternalAppClosedEvent event) {
		searchButton.setEnabled(true);
		searchTextField.setEnabled(true);
	}

	@Override
	public void handleEvent(ExternalAppStartedEvent event) {
		searchButton.setEnabled(false);
		searchTextField.setEnabled(false);
	}
}
