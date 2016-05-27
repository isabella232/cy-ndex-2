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
import java.awt.event.MouseListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import org.cytoscape.application.events.CyShutdownEvent;
import org.cytoscape.application.events.CyShutdownListener;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
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
	private Process electron;

	// WS Client
	private final WSClient client;

	// States
	private Boolean searchBoxClicked = false;

	public NdexMainPanel(final CySwingApplication cySwingApplicationServiceRef) {
		this.cySwingApplicationServiceRef = cySwingApplicationServiceRef;
		this.client = new WSClient(cySwingApplicationServiceRef);
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
		clearPanel.setBackground(Color.decode("#F3FFE2"));
		JPanel searchPanel = new JPanel();
		searchPanel.setBackground(Color.decode("#EB7F00"));
		
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
		searchPanel.add(searchButton);
		buttonPanel.add(searchPanel);
		
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

		searchBox.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!searchBoxClicked) {
					searchBox.setText("");
					searchBoxClicked = true;
				}
			}
		});
		return searchBox;
	}

	private final JButton getClearButton() {
		final JButton clearButton = new JButton("Clear");
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
		final JButton searchButton = new JButton("Search NDEx");
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

		if (this.client.getSocket() == null || this.client.isStopped()) {
			final String dest = "ws://localhost:8025/ws/echo";
			client.start(dest);
		}

		String cmd = "/Users/kono/prog/git/electron-quick-start/NDEx-darwin-x64/NDEx.app/Contents/MacOS/NDEx";

		if (electron != null) {
			System.out.println("RUNNING!!!!!!!! ------------");
			return;
		}

		ObjectMapper mapper = new ObjectMapper();
		InterAppMessage q = new InterAppMessage();
		q.setType("cy3");
		q.setMessage(query);

		this.client.getSocket().sendMessage(mapper.writeValueAsString(q));

		final ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			String threadName = Thread.currentThread().getName();
			System.out.println("Hello " + threadName);
			try {
				electron = Runtime.getRuntime().exec(cmd);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Map alive? " + electron.isAlive());
			while (electron.isAlive()) {
				try {
					Thread.sleep(2000);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("---------- Map Not finished yet: " + electron.isAlive());
			}

			// Bring it back to front
			final JFrame desktop = cySwingApplicationServiceRef.getJFrame();
			desktop.setAlwaysOnTop(true);
			// desktop.toFront();
			desktop.requestFocus();
			try {
				Thread.sleep(200);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			desktop.setAlwaysOnTop(false);

			this.electron = null;
			System.out.println("---------- Map finished: " + electron.isAlive());
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
		if (electron != null) {
			electron.destroyForcibly();
		}

	}
}
