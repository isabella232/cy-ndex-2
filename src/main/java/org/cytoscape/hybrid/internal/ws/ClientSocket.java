package org.cytoscape.hybrid.internal.ws;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import javax.swing.JFrame;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.hybrid.events.InterAppMessage;
import org.cytoscape.hybrid.events.WSHandler;
import org.cytoscape.hybrid.events.WebSocketEvent;
import org.cytoscape.hybrid.internal.login.Credential;
import org.cytoscape.hybrid.internal.login.LoginManager;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebSocket
public class ClientSocket {

	private Session currentSession;

	private final CySwingApplication app;
	private final ObjectMapper mapper;
	private final ExternalAppManager pm;

	private final CyEventHelper eventHelper;

	private static final InterAppMessage ALIVE;

	private final Map<String, WSHandler> handlers;
	
	static {
		ALIVE = InterAppMessage.create()
				.setFrom(InterAppMessage.FROM_CY3)
				.setType(InterAppMessage.TYPE_ALIVE)
				.setBody("Cy3 Client Alive.");
	}

	// private Boolean ignore = false;
	private final CountDownLatch latch = new CountDownLatch(1);
	
	private String application;
	private final LoginManager loginManager;
	

	public ClientSocket(final CySwingApplication app, 
			ExternalAppManager pm, final CyEventHelper eventHelper, final LoginManager loginManager) {
		this.app = app;
		this.pm = pm;
		this.eventHelper = eventHelper;
		this.loginManager = loginManager;

		this.mapper = new ObjectMapper();
		this.handlers = new HashMap<>();
		
		addListener();

		final Timer ping = new Timer();

		ping.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					sendMessage(mapper.writeValueAsString(ALIVE));
				} catch (JsonProcessingException e1) {
					e1.printStackTrace();
				}
			}
		}, 0, 120000);
	}
	
	public void setApplication(final String app) {
		this.application = app;
	}

	private void addListener() {
		final JFrame desktop = app.getJFrame();
		desktop.addWindowListener(new WindowAdapter() {

			@Override
			public void windowActivated(WindowEvent e) {

				System.out.println("* Cy3 Window Activated manually: " + e);

				final InterAppMessage msg = InterAppMessage.create().setType(InterAppMessage.TYPE_FOCUS)
						.setFrom(InterAppMessage.FROM_CY3);
				try {
					sendMessage(mapper.writeValueAsString(msg));
				} catch (JsonProcessingException e1) {
					e1.printStackTrace();
				}
			}
		});
	}

	@OnWebSocketMessage
	public void onText(Session session, String message) throws IOException {

		// Map message into message object
		final InterAppMessage msg = mapper.readValue(message, InterAppMessage.class);

		final String from = msg.getFrom();

		System.out.println("*** CY3 CLIENT: Message received from server:" + message);

		if (msg.getType().equals(InterAppMessage.TYPE_APP)) {
			System.out.println("Electron App type requested: ");
			final InterAppMessage reply = InterAppMessage.create()
					.setType(InterAppMessage.TYPE_APP)
					.setFrom(InterAppMessage.FROM_CY3)
					.setBody(application);
			final Credential cred = loginManager.getLogin();
			if(cred != null) {
				// Add login as optional param
				reply.setOptions(cred);
			}
			
			try {
				sendMessage(mapper.writeValueAsString(reply));
			} catch (JsonProcessingException e1) {
				e1.printStackTrace();
			}
		
		} else if (msg.getType().equals(InterAppMessage.TYPE_CLOSED)) {
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

			System.out.println("**** Electron app focused 2++++++++++++ ");
			// ignore = true;
			final JFrame desktop = app.getJFrame();
			System.out.println("* IsFocused: " + desktop.isFocused());
			System.out.println("* IsActive: " + desktop.isActive());
			System.out.println("* IsShow: " + desktop.isShowing());
			System.out.println("* IsFocusOwner: " + desktop.isFocusOwner());
			System.out.println("* IsVisible: " + desktop.isVisible());
			System.out.println("* IsFocusableWindow: " + desktop.isFocusableWindow());
			System.out.println("* IsisAutoRequestFocus: " + desktop.isAutoRequestFocus());
			
			if (desktop.isFocused() || desktop.isActive()) {
				System.out.println("**** No need to focus ");
			} else {
				System.out.println("**** AOT Bring Cytoscape desktop to front");
				desktop.setAlwaysOnTop(true);
				desktop.toFront();
				desktop.requestFocus();
				desktop.setAlwaysOnTop(false);
				
				// Send success message:
				final InterAppMessage reply = new InterAppMessage();
				reply.setType(InterAppMessage.TYPE_FOCUS_SUCCESS).setFrom(InterAppMessage.FROM_CY3);
				try {
					sendMessage(mapper.writeValueAsString(reply));
				} catch (JsonProcessingException e1) {
					e1.printStackTrace();
				}
			}
		} else if (msg.getType().equals(InterAppMessage.TYPE_FOCUS_SUCCESS)) {
			if (from.equals(InterAppMessage.FROM_CY3)) {
				return;
			}

			// ignore = true;

			final JFrame desktop = app.getJFrame();
			if (desktop.isFocused() || desktop.isActive()) {
				app.getJFrame().toFront();
				return;
			}

			System.out.println("**** Focus Success from NDEX: ");
			app.getJFrame().toFront();
			app.getJFrame().requestFocus();
			app.getJFrame().repaint();
			System.out.println("Finish: ");
			// ignore = false;
		} else {
			// Try handlers
			final WSHandler handler = handlers.get(msg.getType());
			System.out.println("!!!!!!!!Need handler: " + handler);
			System.out.println("!!!!!!!!Need handler2: " + handlers.size());
			if(handler != null) {
				handler.handleMessage(msg, this.currentSession);
			}
			
		}
	}

	@OnWebSocketConnect
	public void onConnect(Session session) {
		System.out.println("@@@@@@@@@@ Cy 3 Client Connected to server");
		this.currentSession = session;
		latch.countDown();
	}

	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
		System.out.println("***Cy3 Client disconnected from server: " + reason + ", " + statusCode);

		currentSession.close();
		currentSession = null;
		// latch.countDown();

		final InterAppMessage msg = new InterAppMessage();
		msg.setFrom(InterAppMessage.FROM_CY3).setType(InterAppMessage.TYPE_CLOSED);
		eventHelper.fireEvent(new WebSocketEvent(this, msg));
	}

	public void sendMessage(String str) {
		if (currentSession == null || currentSession.isOpen() == false) {
			return;
		}

		try {
			this.currentSession.getRemote().sendString(str);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public CountDownLatch getLatch() {
		return latch;
	}
	
	
	public void addHandler(final WSHandler handler) {
		this.handlers.put(handler.getType(), handler);
	}
}
