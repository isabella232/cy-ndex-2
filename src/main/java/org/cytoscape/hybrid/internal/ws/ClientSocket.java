package org.cytoscape.hybrid.internal.ws;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

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

	private Boolean focusFlag = false;

	public ClientSocket(final CySwingApplication app, ExternalAppManager pm, final CyEventHelper eventHelper) {
		this.app = app;
		this.pm = pm;
		this.eventHelper = eventHelper;

		this.mapper = new ObjectMapper();
		addListener();
	}

	private void addListener() {
		final JFrame desktop = app.getJFrame();
		desktop.addWindowListener(new WindowAdapter() {

			@Override
			public void windowActivated(WindowEvent e) {
				System.out.println("=Acttive Cytoscape: " + e);

				if (!focusFlag) {
					focusFlag = true;
					final InterAppMessage msg = new InterAppMessage();
					msg.setType(InterAppMessage.TYPE_FOCUS).setFrom(InterAppMessage.FROM_CY3);
					try {
						sendMessage(mapper.writeValueAsString(msg));
					} catch (JsonProcessingException e1) {
						e1.printStackTrace();
					}
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

		final String from = msg.getFrom();

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
			desktop.requestFocus();
			desktop.toFront();
			desktop.repaint();
			desktop.setAlwaysOnTop(false);

			eventHelper.fireEvent(new WebSocketEvent(this, msg));

		} else if (msg.getType().equals(InterAppMessage.TYPE_CONNECTED)) {
			System.out.println("**** Sending query *****");
			final InterAppMessage reply = new InterAppMessage();
			reply.setType(InterAppMessage.TYPE_QUERY).setBody(pm.getQuery()).setFrom(InterAppMessage.FROM_CY3);
			try {
				sendMessage(mapper.writeValueAsString(reply));
			} catch (JsonProcessingException e1) {
				e1.printStackTrace();
			}
			eventHelper.fireEvent(new WebSocketEvent(this, msg));

		} else if (msg.getType().equals(InterAppMessage.TYPE_FOCUS)) {
			if (from.equals(InterAppMessage.FROM_CY3)) {
				return;
			}
			System.out.println("**** Focus from NDEX: " + app.getJFrame().isAutoRequestFocus());
			app.getJFrame().setAlwaysOnTop(true);
			app.getJFrame().requestFocus();
			app.getJFrame().toFront();
			app.getJFrame().setAlwaysOnTop(false);
			final InterAppMessage reply = new InterAppMessage();
			reply.setType(InterAppMessage.TYPE_FOCUS_SUCCESS).setBody(pm.getQuery()).setFrom(InterAppMessage.FROM_CY3);
			try {
				sendMessage(mapper.writeValueAsString(reply));
			} catch (JsonProcessingException e1) {
				e1.printStackTrace();
			}
		} else if (msg.getType().equals(InterAppMessage.TYPE_FOCUS_SUCCESS)) {
			if (from.equals(InterAppMessage.FROM_CY3)) {
				return;
			}
			System.out.println("**** Focus Success from NDEX: ");
			app.getJFrame().toFront();
			app.getJFrame().requestFocus();
			app.getJFrame().repaint();
			focusFlag = false;
			System.out.println("Finish: ");
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
		System.out.println("***Cy3 DisConnected from server: " + reason + ", " + statusCode);

		// final InterAppMessage msg = new InterAppMessage();
		// msg.setFrom(InterAppMessage.FROM_CY3);
		// msg.setType(InterAppMessage.TYPE_CLOSED);
		// eventHelper.fireEvent(new WebSocketEvent(this, msg));
	}

	public void sendMessage(String str) {
		try {
			session.getRemote().sendString(str);
		} catch (IOException e) {
			pm.kill();
			e.printStackTrace();
		}
	}

	public CountDownLatch getLatch() {
		return latch;
	}
}
