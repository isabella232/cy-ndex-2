package org.cytoscape.fx.internal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import org.cytoscape.application.events.CyShutdownEvent;
import org.cytoscape.application.events.CyShutdownListener;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.fx.internal.ws.ProcessManager;
import org.cytoscape.fx.internal.ws.WSClient;
import org.cytoscape.fx.internal.ws.message.InterAppMessage;

import com.fasterxml.jackson.databind.ObjectMapper;

public class NdexMainPanel extends JPanel implements CytoPanelComponent2, CyShutdownListener {

	private static final long serialVersionUID = 384761556830950601L;

	// Logo Icon
	private static final Icon NDEX_LOGO = new ImageIcon(
			NdexMainPanel.class.getClassLoader().getResource("images/ndex.png"));

	// Fonts used in UI
	private static final Font defFont = new Font("helvatica", Font.PLAIN, 14);
	private static final Font buttonFont = new Font("helvatica", Font.PLAIN, 16);
	private static final Font buttonFont2 = new Font("helvatica", Font.BOLD, 16);

	private final CySwingApplication cySwingApplicationServiceRef;

	// For child process management
	private final ProcessManager pm;

	// WS Client
	private final WSClient client;
	private final ObjectMapper mapper = new ObjectMapper();

	// States
	private Boolean searchBoxClicked = false;

	public NdexMainPanel(final CySwingApplication cySwingApplicationServiceRef, final ProcessManager pm) {
		this.cySwingApplicationServiceRef = cySwingApplicationServiceRef;
		this.pm = pm;

		this.client = new WSClient(cySwingApplicationServiceRef, pm);
		init();
	}

	private final void init() {
		this.setLayout(new BorderLayout());
		final JLabel logo = new JLabel();
		logo.setIcon(NDEX_LOGO);
		logo.setHorizontalAlignment(SwingConstants.CENTER);
		logo.setHorizontalTextPosition(SwingConstants.CENTER);
		final Border paddingBorder = BorderFactory.createEmptyBorder(5, 0, 5, 0);
		logo.setBorder(paddingBorder);
		this.add(logo, BorderLayout.NORTH);

		// Search query
		final JEditorPane searchBox = getSearchBox();
		this.add(searchBox, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(1, 2));

		JPanel clearPanel = new JPanel();
		clearPanel.setBackground(Color.decode("#FAFFEA"));
		JPanel searchPanel = new JPanel();
		searchPanel.setBackground(Color.decode("#FF4081"));

		final JButton clearButton = getClearButton();
		clearButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				searchBox.setText("");
			}
		});
		clearPanel.add(clearButton);
		buttonPanel.add(clearPanel);

		final JButton searchButton = getButton();
		searchButton.setSize(buttonPanel.getSize());
		searchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					search(searchBox.getText());
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
//		searchButton.setEnabled(false);
		searchPanel.add(searchButton);
		buttonPanel.add(searchPanel);
		
		searchBox.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!searchBoxClicked) {
					searchBox.setText("");
					searchBoxClicked = true;
//					searchButton.setEnabled(true);
				}
			}
		});

		this.add(buttonPanel, BorderLayout.SOUTH);

		this.setBackground(Color.WHITE);
	}

	private final JEditorPane getSearchBox() {
		JEditorPane searchBox = new JEditorPane();
		searchBox.setFont(defFont);
		searchBox.setBackground(Color.decode("#FFFFFF"));
		searchBox.setForeground(Color.decode("#666666"));
		final Border paddingBorder3 = BorderFactory.createEmptyBorder(7, 7, 7, 7);
		searchBox.setBorder(paddingBorder3);

		searchBox.setText("Enter search terms here...");

		return searchBox;
	}

	private final JButton getClearButton() {
		final JButton clearButton = new JButton("CLEAR");
		clearButton.setOpaque(false);
		clearButton.setForeground(Color.decode("#777777"));
		clearButton.setHorizontalAlignment(SwingConstants.CENTER);
		clearButton.setHorizontalTextPosition(SwingConstants.CENTER);
		clearButton.setFont(buttonFont);
		final Border paddingBorder2 = BorderFactory.createEmptyBorder(10, 0, 10, 0);
		clearButton.setBorder(paddingBorder2);

		return clearButton;
	}

	private final JButton getButton() {
		final JButton searchButton = new JButton("SEARCH");
		searchButton.addMouseListener(new MouseAdapter() {

			private Color original;

			@Override
			public void mouseExited(MouseEvent e) {
				JButton button = (JButton) e.getComponent();
				Container panel = button.getParent();
				panel.setBackground(original);
				button.setFont(buttonFont);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				JButton button = (JButton) e.getComponent();
				Container panel = button.getParent();
				original = panel.getBackground();
				final Color brighter = original.brighter();
				panel.setBackground(brighter);
				button.setFont(buttonFont2);
			}
		});

		searchButton.setToolTipText("Click to show search result in a new window...");
		searchButton.setOpaque(false);
		searchButton.setForeground(Color.white);
		searchButton.setHorizontalAlignment(SwingConstants.CENTER);
		searchButton.setHorizontalTextPosition(SwingConstants.CENTER);
		searchButton.setFont(buttonFont);
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

		String cmd = "/Users/kono/CytoscapeConfiguration/native/NDEx.app/Contents/MacOS/NDEx";

		if (pm.isActive()) {
			final InterAppMessage focus = new InterAppMessage();
			focus.setFrom(InterAppMessage.FROM_CY3);
			focus.setType(InterAppMessage.TYPE_FOCUS);
			this.client.getSocket().sendMessage(mapper.writeValueAsString(focus));
			return;
		}

		final ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			try {
				pm.setProcess(Runtime.getRuntime().exec(cmd));
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("---------- Electron is running");
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
}
