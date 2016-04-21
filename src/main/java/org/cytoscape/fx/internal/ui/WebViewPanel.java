package org.cytoscape.fx.internal.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.Icon;
import javax.swing.JPanel;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.task.NetworkTaskFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class WebViewPanel extends JPanel implements CytoPanelComponent {

	private static final long serialVersionUID = -6270382855252926155L;

	private JFXPanel fxPanel;
	private final URL startPageUrl;

	private final String content;

	private final NetworkTaskFactory fitContent;
	
	public WebViewPanel(NetworkTaskFactory fitContent) {
		this.fitContent = fitContent;
		
		this.startPageUrl = WebViewPanel.class.getClassLoader().getResource("pages/index.html");
		initComponents();

		try {
			this.content = loadStartPage();
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalStateException("Could not load resource HTML file.", e);
		}
	}

	private final void initComponents() {
		this.setLayout(new BorderLayout());
		
		this.fxPanel = new JFXPanel();
		this.add(fxPanel, BorderLayout.CENTER);
		
		this.setPreferredSize(new Dimension(800, 700));
	}

	private final String loadStartPage() throws IOException {
		final StringBuilder builder = new StringBuilder();

		URLConnection yc = startPageUrl.openConnection();
		BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

		String inputLine;
		while ((inputLine = in.readLine()) != null)
			builder.append(inputLine);

		in.close();

		return builder.toString();
	}

	public void load() {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				WebView view = new WebView();
				WebEngine engine = view.getEngine();

				final EventListener testListener = new EventListener() {
					public void handleEvent(Event ev) {
						System.out.println("Message clicked: " + ev.toString());
					}
				};
				
				final EventListener layoutListener = new EventListener() {
					public void handleEvent(Event ev) {
						System.out.println("Apply layout: " + ev.toString());
					}
				};
				final EventListener fitContentListener = new EventListener() {
					public void handleEvent(Event ev) {
						System.out.println("Fit content: " + ev.toString());
//						fitContent.createTaskIterator(null).next().run(null);;
					}
				};

				engine.getLoadWorker().stateProperty().addListener(new javafx.beans.value.ChangeListener<State>() {
					public void changed(ObservableValue ov, State oldState, State newState) {
						if (newState == State.SUCCEEDED) {
							System.out.println("Page Loaded!!!!!!!!!!!!!");
							Document doc = engine.getDocument();
							System.out.println(doc.toString());

							Element el1 = doc.getElementById("test");
							((EventTarget) el1).addEventListener("click", testListener, false);
							
							Element el2 = doc.getElementById("apply-layout");
							((EventTarget) el2).addEventListener("click", layoutListener, false);
							
							Element el3 = doc.getElementById("fit-content");
							((EventTarget) el3).addEventListener("click", fitContentListener, false);
						}
					}
				});

				System.out.println("Content : \n" + content);
//				engine.loadContent(content);
//				engine.load("file:///Users/kono/pages/index.html");
//				engine.load("http://goo.gl/VQswUq");
				engine.load("http://html5test.com/");
				fxPanel.setScene(new Scene(view));
			}
		});

	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.EAST;
	}

	@Override
	public String getTitle() {
		return "JavaFX WebView Sample";
	}

	@Override
	public Icon getIcon() {
		return null;
	}
}