package org.cytoscape.hybrid.internal.ui;

import static org.cytoscape.hybrid.internal.ui.UiTheme.DEF_FONT;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import org.cytoscape.hybrid.events.InterAppMessage;
import org.cytoscape.hybrid.internal.ws.ExternalAppManager;
import org.cytoscape.hybrid.internal.ws.WSClient;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SearchBox extends JPanel {

	private static final long serialVersionUID = 5216512744558942600L;

	private static final Icon NDEX_LOGO = new ImageIcon(
			SearchBox.class.getClassLoader().getResource("images/ndex-logo.png"));

	private static final Icon ICON_SEARCH = new ImageIcon(
			SearchBox.class.getClassLoader().getResource("images/search.png"));
	private static final Icon ICON_SETTINGS = new ImageIcon(
			SearchBox.class.getClassLoader().getResource("images/settings.png"));

	private static final Dimension PANEL_SIZE = new Dimension(400, 60);
	private static final Dimension TEXT_AREA_SIZE = new Dimension(250, 50);
	private static final Color BORDER_COLOR = new Color(200, 200, 200);
	private static final Color TITLE_COLOR = new Color(50, 50, 50);
	private static final Font TITLE_FONT = new Font("SansSerif", Font.PLAIN, 10);

	private static final Dimension BUTTON_SIZE = new Dimension(40, 40);

	private static final String PLACEHOLDER = "Enter search terms here...";

	private final JButton iconLabel;
	private final JTextField searchTextField;
	private final JButton searchButton;
	private final JButton settingButton;

	// WS Client
	private final WSClient client;
	private final ObjectMapper mapper;

	// States
	private Boolean searchBoxClicked = false;

	// For child process management
	private final ExternalAppManager pm;

	private final String command;

	public SearchBox(final WSClient client, final ExternalAppManager pm, String command) {

		this.mapper = new ObjectMapper();

		this.client = client;
		this.pm = pm;
		this.command = command;

		this.setPreferredSize(PANEL_SIZE);
		this.setSize(PANEL_SIZE);

		this.iconLabel = new JButton();
		this.iconLabel.setSize(BUTTON_SIZE);
		this.iconLabel.setIcon(NDEX_LOGO);
		this.iconLabel.setToolTipText("Data Source: NDEx");

		this.searchTextField = new JTextField();
		this.searchTextField.setFont(DEF_FONT);
		this.searchTextField.setBackground(Color.decode("#FFFFFF"));
		this.searchTextField.setForeground(Color.decode("#555555"));
		this.searchTextField.setPreferredSize(TEXT_AREA_SIZE);
		this.searchTextField.setSize(TEXT_AREA_SIZE);
		this.searchTextField.setText(PLACEHOLDER);
		this.searchTextField.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {

				if (!isEnabled()) {
					MessageUtil.reauestExternalAppFocus(client);
					return;
				}

				if (!searchBoxClicked) {
					searchTextField.setText("");
					searchBoxClicked = true;
				}
			}
		});

		this.searchButton = new JButton();
		this.settingButton = new JButton();
		this.searchButton.setIcon(ICON_SEARCH);
		this.settingButton.setIcon(ICON_SETTINGS);
		this.settingButton.setSize(BUTTON_SIZE);
		this.searchButton.setSize(BUTTON_SIZE);

		this.searchButton.setBackground(Color.white);
		this.settingButton.setBackground(Color.white);

		this.searchButton.setToolTipText("Start search (opens new window)");
		this.settingButton.setToolTipText("Open setting window...");
		
		this.searchButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					search(searchTextField.getText());
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		
		this.settingButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					setting();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});

		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(1, 2));
		buttonPanel.add(searchButton);
		buttonPanel.add(settingButton);

		setLayout(new BorderLayout());
		add(iconLabel, BorderLayout.WEST);
		add(searchTextField, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.EAST);

		final LineBorder border = new LineBorder(BORDER_COLOR, 1);
		final TitledBorder title = new TitledBorder(border, "Search Networks", TitledBorder.LEFT, TitledBorder.TOP);
		title.setTitleFont(TITLE_FONT);
		title.setTitleColor(TITLE_COLOR);
		setBorder(title);
	}

	
	private final void search(String query) throws Exception {
		// Use empty String if default text is used.
		if (query.equals(PLACEHOLDER)) {
			query = "";
		}

		pm.setQuery(query);
		
		execute("ndex");
	}	

	private final void setting() {
		try {
			execute("ndex-login");
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	private final void execute(final String app) throws Exception {

		final String dest = "ws://localhost:8025/ws/echo";
		client.start(dest);

		if (pm.isActive()) {
			final InterAppMessage focus = InterAppMessage.create().setFrom(InterAppMessage.FROM_CY3)
					.setType(InterAppMessage.TYPE_FOCUS);
			this.client.getSocket().sendMessage(mapper.writeValueAsString(focus));
			return;
		}

		final ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			try {
				// Set application type:
				this.client.getSocket().setApplication(app);
				pm.setProcess(Runtime.getRuntime().exec(command));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
	
}
