package org.cytoscape.hybrid.internal.ui;

import static org.cytoscape.hybrid.internal.ui.UiTheme.DEF_FONT;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.border.Border;

import org.cytoscape.hybrid.events.InterAppMessage;
import org.cytoscape.hybrid.internal.ws.ExternalAppManager;
import org.cytoscape.hybrid.internal.ws.WSClient;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SearchPane extends JEditorPane implements PropertyChangeListener {

	private static final long serialVersionUID = -6181889059129429760L;
	
	protected static final String ACTION_CLEAR = "clear";
	protected static final String ACTION_SEARCH = "search";
	
	// WS Client
	private final WSClient client;
	private final ObjectMapper mapper;

	// States
	private Boolean searchBoxClicked = false;

	// For child process management
	private final ExternalAppManager pm;

	private final String command;

	public SearchPane(final WSClient client, final ExternalAppManager pm, String command) {

		this.mapper = new ObjectMapper();

		this.client = client;
		this.pm = pm;
		this.command = command;

		setFont(DEF_FONT);
		setBackground(Color.decode("#FFFFFF"));
		setForeground(Color.decode("#555555"));

		final Border paddingBorder3 = BorderFactory.createEmptyBorder(7, 7, 7, 7);
		setBorder(paddingBorder3);

		setText("Enter search terms here...");

		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (!searchBoxClicked) {
					setText("");
					
					searchBoxClicked = true;
				}
			}
		});

	}

	private final void search(final String query) throws Exception {

		pm.setQuery(query);

		final String dest = "ws://localhost:8025/ws/echo";
		client.start(dest);

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
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals(ACTION_CLEAR)) {
			this.setText("");
		} else if (evt.getPropertyName().equals(ACTION_SEARCH)){
			setEnabled(false);
			try {
				search(getText());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
