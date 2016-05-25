package org.cytoscape.fx.internal.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
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

public class NdexMainPanel extends JPanel implements CytoPanelComponent2, CyShutdownListener {

	// Logo Icon
	private static final Icon NDEX_LOGO = new ImageIcon(
			NdexMainPanel.class.getClassLoader().getResource("images/ndex.png"));

	// Fonts used in UI
	private static final Font defFont = new Font("helvatica", Font.PLAIN, 12);
	private static final Font buttonFont = new Font("helvatica", Font.PLAIN, 14);
	private static final Font buttonFont2 = new Font("helvatica", Font.BOLD, 14);

	private final CySwingApplication cySwingApplicationServiceRef;

	// For child process management
	private Process electron;

	public NdexMainPanel(final CySwingApplication cySwingApplicationServiceRef) {
		this.cySwingApplicationServiceRef = cySwingApplicationServiceRef;
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

		JPanel buttonPanel = new JPanel();
		buttonPanel.setBackground(Color.decode("#EB7F00"));
		final JButton searchButton = getButton();
		searchButton.setSize(buttonPanel.getSize());
		buttonPanel.add(searchButton, BorderLayout.CENTER);
		this.add(buttonPanel, BorderLayout.SOUTH);

		JEditorPane searchBox = new JEditorPane();
		searchBox.setFont(defFont);

		searchBox.setBackground(Color.decode("#1695A3"));
		searchBox.setForeground(Color.decode("#EEEEEE"));
		final Border paddingBorder3 = BorderFactory.createEmptyBorder(7, 7, 7, 7);
		searchBox.setBorder(paddingBorder3);

		searchBox.setText("Enter search terms here...");
		this.add(searchBox, BorderLayout.CENTER);

		this.setBackground(Color.WHITE);
	}

	private final JButton getButton() {
		final JButton searchButton = new JButton("Search NDEx");
		searchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					search();
				} catch (IOException | InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});

		searchButton.addMouseListener(new MouseListener() {

			private Color original;

			@Override
			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub

			}

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

			@Override
			public void mouseClicked(MouseEvent e) {
				// TODO Auto-generated method stub

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

	private final void search() throws IOException, InterruptedException {

		System.out.println("---------- Opening loading ------------");
		String cmd = "/Users/kono/prog/git/electron-quick-start/NDEx-darwin-x64/NDEx.app/Contents/MacOS/NDEx";
		System.out.println("---------- Map opened ------------");

		if (electron != null) {
			System.out.println("RUNNING!!!!!!!! ------------");
			return;
		}

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
		if(electron != null) {
			electron.destroyForcibly();
		}
		
	}
}
