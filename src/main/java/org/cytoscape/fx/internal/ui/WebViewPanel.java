package org.cytoscape.fx.internal.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.Icon;

import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedEvent;
import org.cytoscape.application.swing.events.CytoPanelComponentSelectedListener;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class WebViewPanel extends JFXPanel implements CytoPanelComponent2, CytoPanelComponentSelectedListener {

	private static final long serialVersionUID = -6270382855252926155L;

	private static final Integer DEF_WIDTH = 1200;

	// JavaFX panel compatible with Swing
	private final String indexPageUrl;

	private Integer width = DEF_WIDTH;
	private Dimension dimension;
	private WebView view;
	private Scene scene;

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

		dimension = new Dimension(width, 600);
		this.setLayout(new BorderLayout());
		this.setPreferredSize(dimension);
		
		Platform.runLater(() -> load());
	}

	public final void load() {
		System.out.println("============== Calling load");
		this.view = new WebView();
		System.out.println("============== load2");
		final WebEngine engine = view.getEngine();
		System.out.println("WebView Engine Version: " + engine.getUserAgent());
		engine.load(this.indexPageUrl);
		this.scene = new Scene(view);
		this.setScene(scene);
		System.out.println("============== load finished ==============");
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
//		// TODO Auto-generated method stub
//		final CytoPanel panel = evt.getCytoPanel();
//		final Component selected = panel.getSelectedComponent();
//
//		if (selected == this) {
//			System.out.println("selected!!!!!!!!!: " + panel.getThisComponent());
//
//			// Find split pane
//			final Component parent = panel.getThisComponent().getParent();
//
//			if (parent instanceof JSplitPane) {
//				final JSplitPane pane = (JSplitPane) parent;
//				pane.setDividerLocation(0.65d);
//			}
//
//			System.out.println(this.fxPanel.getSize());
//			System.out.println(this.view.getHeight());
//			System.out.println("Panel state: ");
//
//		} else {
//			System.out.println("NOT!!!!!!!!!: ");
//
//		}
//
	}
}
