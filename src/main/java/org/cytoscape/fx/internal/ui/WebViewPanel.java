package org.cytoscape.fx.internal.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedEvent;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedListener;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class WebViewPanel extends JPanel implements CytoPanelComponent2, CytoPanelComponentSelectedListener {

	private static final long serialVersionUID = -6270382855252926155L;

	// JavaFX panel compatible with Swing
	private final JFXPanel fxPanel;

	private final String indexPageUrl;

	public WebViewPanel(final String indexPageLocation) {

		// Validate the URL
		if (indexPageLocation == null) {
			throw new IllegalArgumentException("Start page location should not nu non-null URL string.");
		}
		try {
			URL testUrl = new URL(indexPageLocation);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Invalid file location: " + indexPageLocation);
		}

		this.indexPageUrl = indexPageLocation;

		this.setLayout(new BorderLayout());
		this.fxPanel = new JFXPanel();
		this.add(fxPanel, BorderLayout.CENTER);
		fxPanel.setPreferredSize(new Dimension(1200, 400));
		fxPanel.setSize(new Dimension(1200, 400));

		this.setPreferredSize(new Dimension(1200, 400));
		this.setSize(new Dimension(1200, 400));
	}

	public void loadPage() {
		Platform.runLater(() -> load());
	}

	private final void load() {
		WebView view = new WebView();
		WebEngine engine = view.getEngine();
		System.out.println("Engine Version: " + engine.getUserAgent());

		engine.load(this.indexPageUrl);
		fxPanel.setScene(new Scene(view));
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.WEST;
	}

	@Override
	public String getTitle() {
		return "NDEx Selector";
	}

	@Override
	public Icon getIcon() {
		return null;
	}

	@Override
	public String getIdentifier() {
		return "org.ndex.Valet";
	}

	@Override
	public void handleEvent(CytoPanelComponentSelectedEvent evt) {
		// TODO Auto-generated method stub
		final CytoPanel panel = evt.getCytoPanel();
		final Component selected = panel.getSelectedComponent();

		if (selected == this) {
			System.out.println("selected!!!!!!!!!");
			// Find split pane
			final JSplitPane pane = (JSplitPane) panel.getThisComponent().getParent();
			pane.setDividerLocation(0.7d);
		} else {
			System.out.println("NOT!!!!!!!!!: ");

		}

	}
}
