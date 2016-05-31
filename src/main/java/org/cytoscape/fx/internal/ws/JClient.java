package org.cytoscape.fx.internal.ws;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.swing.JFrame;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.fx.internal.ws.message.InterAppMessage;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebSocket
public class JClient {

	private Session session;
	private final CySwingApplication app;
	private final ObjectMapper mapper;
	private final ProcessManager pm;

	public JClient(final CySwingApplication app, ProcessManager pm) {
		this.app = app;
		this.pm = pm;
		this.mapper = new ObjectMapper();
		System.out.println("$$ Setting listener...");
		addListener();
		System.out.println("$$ DONE@@@@@@@@@");
	}

	private void addListener() {
		final JFrame desktop = app.getJFrame();
		
		desktop.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				System.out.println("**** Focusd lost *****");
			}

			@Override
			public void focusGained(FocusEvent e) {
				System.out.println("**** Focusd *****");
				final InterAppMessage msg = new InterAppMessage();
				msg.setType(InterAppMessage.TYPE_FOCUS);
				msg.setFrom(InterAppMessage.FROM_CY3);
				try {
					sendMessage(mapper.writeValueAsString(msg));
				} catch (JsonProcessingException e1) {
					e1.printStackTrace();
				}
			}
		});
	}

	private CountDownLatch latch = new CountDownLatch(1);

	@OnWebSocketMessage
	public void onText(Session session, String message) throws IOException {
		System.out.println("*** CY3 CLIENT: Message received from server:" + message);

		final InterAppMessage msg = mapper.readValue(message, InterAppMessage.class);

		if (msg.getType().equals(InterAppMessage.TYPE_CLOSED)) {
			System.out.println("Electron closed: ");
			pm.kill();
			app.getJFrame().setAlwaysOnTop(true);
			app.getJFrame().requestFocus();
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			app.getJFrame().setAlwaysOnTop(false);
		} else if (msg.getType().equals("connected")) {
			System.out.println("**** Sending query *****");
			final InterAppMessage reply = new InterAppMessage();
			reply.setType(InterAppMessage.TYPE_QUERY);
			reply.setBody(pm.getQuery());
			reply.setFrom(InterAppMessage.FROM_CY3);
			try {
				sendMessage(mapper.writeValueAsString(reply));
			} catch (JsonProcessingException e1) {
				e1.printStackTrace();
			}

		} else if(msg.getType().equals(InterAppMessage.TYPE_FOCUS) && msg.getFrom().equals(InterAppMessage.FROM_NDEX)){
			System.out.println("**** Focus from NDEX: " + app.getJFrame().isAutoRequestFocus());
			app.getJFrame().requestFocus();
			app.getJFrame().toFront();
		}
	}

	@OnWebSocketConnect
	public void onConnect(Session session) {
		System.out.println("Connected to server");
		this.session = session;
		latch.countDown();
	}

	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
		System.out.println("**************DISConnected from server: " + reason + ", " + statusCode);
	}

	public void sendMessage(String str) {
		try {
			session.getRemote().sendString(str);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public CountDownLatch getLatch() {
		return latch;
	}

}
