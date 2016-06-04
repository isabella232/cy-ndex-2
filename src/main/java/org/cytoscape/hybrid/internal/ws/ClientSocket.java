package org.cytoscape.hybrid.internal.ws;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.swing.JFrame;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.hybrid.events.InterAppMessage;
import org.cytoscape.hybrid.events.WebSocketEvent;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebSocket
public class ClientSocket {

	private Session session;
	private final CySwingApplication app;
	private final ObjectMapper mapper;
	private final ExternalAppManager pm;
	
	private final CyEventHelper eventHelper;

	public ClientSocket(final CySwingApplication app, ExternalAppManager pm, final CyEventHelper eventHelper) {
		this.app = app;
		this.pm = pm;
		this.eventHelper = eventHelper;
		
		this.mapper = new ObjectMapper();
		addListener();
	}

	private void addListener() {
		final JFrame desktop = app.getJFrame();
		
		desktop.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {}

			@Override
			public void focusGained(FocusEvent e) {
				final InterAppMessage msg = new InterAppMessage();
				msg.setType(InterAppMessage.TYPE_FOCUS).setFrom(InterAppMessage.FROM_CY3);
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

		// Map message into message object
		final InterAppMessage msg = mapper.readValue(message, InterAppMessage.class);

		if (msg.getType().equals(InterAppMessage.TYPE_CLOSED)) {
			System.out.println("Electron closed: ");
			pm.kill();
			final JFrame desktop = app.getJFrame();
			desktop.setAlwaysOnTop(true);
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			desktop.toFront();
			desktop.requestFocus();
			desktop.setAlwaysOnTop(false);
			
			eventHelper.fireEvent(new WebSocketEvent(this, msg));
			
		} else if (msg.getType().equals(InterAppMessage.TYPE_CONNECTED)) {
			System.out.println("**** Sending query *****");
			final InterAppMessage reply = new InterAppMessage();
			reply.setType(InterAppMessage.TYPE_QUERY)
				.setBody(pm.getQuery())
				.setFrom(InterAppMessage.FROM_CY3);
			try {
				sendMessage(mapper.writeValueAsString(reply));
			} catch (JsonProcessingException e1) {
				e1.printStackTrace();
			}
			eventHelper.fireEvent(new WebSocketEvent(this, msg));

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
