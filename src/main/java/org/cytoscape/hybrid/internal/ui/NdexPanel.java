package org.cytoscape.hybrid.internal.ui;

import static org.cytoscape.hybrid.internal.ui.UiTheme.COLOR_DISABLED;
import static org.cytoscape.hybrid.internal.ui.UiTheme.COLOR_ENABLED;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.io.File;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.events.CyShutdownEvent;
import org.cytoscape.application.events.CyShutdownListener;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.hybrid.events.InterAppMessage;
import org.cytoscape.hybrid.events.WebSocketEvent;
import org.cytoscape.hybrid.events.WebSocketEventListener;
import org.cytoscape.hybrid.internal.electron.NativeAppInstaller;
import org.cytoscape.hybrid.internal.ws.ExternalAppManager;
import org.cytoscape.hybrid.internal.ws.WSClient;

public class NdexPanel extends JPanel
		implements CytoPanelComponent2, CyShutdownListener, WebSocketEventListener {

	private static final long serialVersionUID = 384761556830950601L;

	// Panels in this component
	private JPanel logoPanel;
	private ButtonPanel buttonPanel;

	private SearchPane searchPane;
	
	private final ExternalAppManager pm;
	private final WSClient client;


	public NdexPanel(final NativeAppInstaller installer, final ExternalAppManager pm, final WSClient client) {
		this.pm = pm;
		
		this.client = client;
		
		init(client, pm, installer.getCommand());
	}


	private final void init(WSClient client, ExternalAppManager pm, String command) {
		// Basic setup
		this.setLayout(new BorderLayout());
		this.setBackground(Color.WHITE);

		// Logo Panel
		this.logoPanel = new LogoPanel();
		this.add(logoPanel, BorderLayout.NORTH);

		// Search Panel
		searchPane = new SearchPane(client, pm, command);
		final JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(searchPane);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(null);
		this.add(scrollPane, BorderLayout.CENTER);

		this.buttonPanel = new ButtonPanel();
		this.buttonPanel.addPropertyChangeListener(searchPane);
		this.add(buttonPanel, BorderLayout.SOUTH);
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
		pm.kill();
	}

	@Override
	public void handleEvent(WebSocketEvent e) {
		final InterAppMessage msg = e.getMessage();

		if (msg.getType().equals(InterAppMessage.TYPE_CONNECTED)) {
			this.searchPane.setEnabled(false);
			this.searchPane.setBackground(COLOR_DISABLED);
			this.logoPanel.setBackground(COLOR_DISABLED);
			this.buttonPanel.setButtonsEnabled(false);
		} else if (msg.getType().equals(InterAppMessage.TYPE_CLOSED)) {
			this.searchPane.setEnabled(true);
			this.searchPane.setBackground(COLOR_ENABLED);
			this.logoPanel.setBackground(COLOR_ENABLED);
			this.buttonPanel.setButtonsEnabled(true);
			pm.kill();
			System.out.println("** WS Event: CLOSED");
			this.searchPane.setEnabled(true);
			this.searchPane.setEditable(true);
			System.out.println("** Editor pane enabled: " + this.searchPane.isEnabled());
		} else {
			System.out.println("** WS Event: No need to respond: " + msg);
			return;
		}
		
	}

}
