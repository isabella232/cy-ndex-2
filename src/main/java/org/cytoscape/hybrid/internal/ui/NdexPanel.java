package org.cytoscape.hybrid.internal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.events.CyShutdownEvent;
import org.cytoscape.application.events.CyShutdownListener;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.hybrid.events.InterAppMessage;
import org.cytoscape.hybrid.events.WebSocketEvent;
import org.cytoscape.hybrid.events.WebSocketEventListener;
import org.cytoscape.hybrid.internal.ws.ExternalAppManager;
import org.cytoscape.hybrid.internal.ws.WSClient;

import com.fasterxml.jackson.databind.ObjectMapper;

public class NdexPanel extends JPanel implements CytoPanelComponent2, CyShutdownListener, WebSocketEventListener {

	private static final long serialVersionUID = 384761556830950601L;

	// Logo Icon
	private static final Icon NDEX_LOGO = new ImageIcon(
			NdexPanel.class.getClassLoader().getResource("images/ndex.png"));

	// Fonts used in UI
	private static final Font DEF_FONT = new Font("helvatica", Font.PLAIN, 14);
	private static final Font BUTTON_FONT1 = new Font("helvatica", Font.PLAIN, 16);
	private static final Font BUTTON_FONT2 = new Font("helvatica", Font.BOLD, 16);

	private static final Color COLOR_DISABLED = new Color(248, 248, 248);
	private static final Color COLOR_ENABLED = Color.WHITE;

	private static final String NATIVE_APP_LOCATION = "native";

	private final CyApplicationConfiguration appConfig;

	// For child process management
	private final ExternalAppManager pm;

	// WS Client
	private final WSClient client;
	private final ObjectMapper mapper = new ObjectMapper();

	// States
	private Boolean searchBoxClicked = false;

	// Components
	private JEditorPane searchBox;
	private JLabel logo;

	private final String command;

	public NdexPanel(final CyApplicationConfiguration appConfig, final ExternalAppManager pm, final WSClient client) {
		this.appConfig = appConfig;
		this.pm = pm;

		this.client = client;

		final File configLocation = this.appConfig.getConfigurationDirectoryLocation();
		final File electronAppDirectory = new File(configLocation, NATIVE_APP_LOCATION);
		this.command = createPlatformDependentCommand(electronAppDirectory);

		init();
	}

	private final String createPlatformDependentCommand(final File configLocation) {
		final String os = System.getProperty("os.name").toLowerCase();

		File f = null;
		if (os.contains("mac")) {
			// Mac OS X
			f = new File(configLocation, "NDEx.app/Contents/MacOS/NDEx");
		} else if (os.contains("win")) {
			// Windows
			f = new File(configLocation, "NDEx.app/Contents/MacOS/NDEx");
		} else {
			// Linux
		}

		System.out.println("\n\nNDEx Command: " + f.getAbsolutePath());
		return f.getAbsolutePath();
	}

	private final void init() {
		this.setLayout(new BorderLayout());
		logo = new JLabel();
		logo.setOpaque(false);
		logo.setIcon(NDEX_LOGO);
		logo.setHorizontalAlignment(SwingConstants.CENTER);
		logo.setHorizontalTextPosition(SwingConstants.CENTER);
		final Border paddingBorder = BorderFactory.createEmptyBorder(5, 0, 5, 0);
		logo.setBorder(paddingBorder);
		this.add(logo, BorderLayout.NORTH);

		// Search query
		this.searchBox = getSearchBox();
		this.add(searchBox, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(1, 2));

		JPanel clearPanel = new JPanel();
		clearPanel.setBackground(Color.decode("#FAFFEA"));
		JPanel searchPanel = new JPanel();
		searchPanel.setBackground(Color.decode("#FF4081"));

		final JLabel clearButton = getClearButton();
		clearButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				searchBox.setText("");
			}
		});
		clearPanel.add(clearButton);
		buttonPanel.add(clearPanel);

		final JLabel searchButton = getButton();
		searchButton.setSize(buttonPanel.getSize());
		searchButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					searchBox.setEnabled(false);
					search(searchBox.getText());
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		// searchButton.setEnabled(false);
		searchPanel.add(searchButton);
		buttonPanel.add(searchPanel);

		searchBox.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!searchBoxClicked) {
					searchBox.setText("");
					searchBoxClicked = true;
					// searchButton.setEnabled(true);
				}
			}
		});

		this.add(buttonPanel, BorderLayout.SOUTH);

		this.setBackground(Color.WHITE);
	}

	private final JEditorPane getSearchBox() {
		JEditorPane searchBox = new JEditorPane();
		searchBox.setFont(DEF_FONT);
		searchBox.setBackground(Color.decode("#FFFFFF"));
		searchBox.setForeground(Color.decode("#666666"));
		final Border paddingBorder3 = BorderFactory.createEmptyBorder(7, 7, 7, 7);
		searchBox.setBorder(paddingBorder3);

		searchBox.setText("Enter search terms here...");

		return searchBox;
	}

	private final JLabel getClearButton() {
		final JLabel clearButton = new JLabel("CLEAR");
		clearButton.setOpaque(false);
		clearButton.setForeground(Color.decode("#777777"));
		clearButton.setHorizontalAlignment(SwingConstants.CENTER);
		clearButton.setHorizontalTextPosition(SwingConstants.CENTER);
		clearButton.setFont(BUTTON_FONT1);
		final Border paddingBorder2 = BorderFactory.createEmptyBorder(10, 0, 10, 0);
		clearButton.setBorder(paddingBorder2);

		return clearButton;
	}

	private final JLabel getButton() {
		final JLabel searchButton = new JLabel("SEARCH");
		searchButton.addMouseListener(new MouseAdapter() {

			private Color original;

			@Override
			public void mouseExited(MouseEvent e) {
				final Component button = e.getComponent();
				Container panel = button.getParent();
				panel.setBackground(original);
				button.setFont(BUTTON_FONT1);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				Component button = e.getComponent();
				Container panel = button.getParent();
				original = panel.getBackground();
				final Color brighter = original.brighter();
				panel.setBackground(brighter);
				button.setFont(BUTTON_FONT2);
			}
		});

		searchButton.setToolTipText("Click to show search result in a new window...");
		searchButton.setOpaque(false);
		searchButton.setForeground(Color.white);
		searchButton.setHorizontalAlignment(SwingConstants.CENTER);
		searchButton.setHorizontalTextPosition(SwingConstants.CENTER);
		searchButton.setFont(BUTTON_FONT1);
		final Border paddingBorder2 = BorderFactory.createEmptyBorder(10, 0, 10, 0);
		searchButton.setBorder(paddingBorder2);

		return searchButton;
	}

	private final void search(final String query) throws Exception {

		pm.setQuery(query);

		if (this.client.getSocket() == null || this.client.isStopped()) {
			final String dest = "ws://localhost:8025/ws/echo";
			client.start(dest);
		}

		if (pm.isActive()) {
			final InterAppMessage focus = new InterAppMessage();
			focus.setFrom(InterAppMessage.FROM_CY3).setType(InterAppMessage.TYPE_FOCUS);
			this.client.getSocket().sendMessage(mapper.writeValueAsString(focus));
			return;
		}

		final ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			try {
				pm.setProcess(Runtime.getRuntime().exec(command));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.SOUTH_WEST;
	}

	@Override
	public String getTitle() {
		return "NDEx";
	}

	@Override
	public Icon getIcon() {
		return null;
	}

	@Override
	public String getIdentifier() {
		return "org.ndex.Main";
	}

	@Override
	public void handleEvent(CyShutdownEvent evt) {
		// Force to kill Ndex Valet process
		pm.kill();
	}

	@Override
	public void handleEvent(WebSocketEvent e) {
		final InterAppMessage msg = e.getMessage();
		System.out.println("** WS Event: " + msg);

		if (msg.getType().equals(InterAppMessage.TYPE_CONNECTED)) {
			this.searchBox.setEnabled(false);
			this.searchBox.setBackground(COLOR_DISABLED);
			this.logo.setBackground(COLOR_DISABLED);
		} else if (msg.getType().equals(InterAppMessage.TYPE_CLOSED)) {
			this.searchBox.setEnabled(true);
			this.searchBox.setBackground(COLOR_ENABLED);
			this.logo.setBackground(COLOR_ENABLED);
		}
	}
}
