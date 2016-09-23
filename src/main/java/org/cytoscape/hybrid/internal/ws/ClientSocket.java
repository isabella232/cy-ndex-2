package org.cytoscape.hybrid.internal.ws;

import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
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
import org.cytoscape.property.CyProperty;
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
	
	private boolean appStarted = false;
	
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

	private final String cyrestPort;
	

	public ClientSocket(final CySwingApplication app, 
			ExternalAppManager pm, final CyEventHelper eventHelper, 
			final LoginManager loginManager, final CyProperty<Properties> props) {
		this.app = app;
		this.pm = pm;
		this.eventHelper = eventHelper;
		this.loginManager = loginManager;

		this.mapper = new ObjectMapper();
		this.handlers = new HashMap<>();
		
		this.cyrestPort = props.getProperties().get("rest.port").toString();
		
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
				final InterAppMessage msg = InterAppMessage.create().setType(InterAppMessage.TYPE_FOCUS)
						.setFrom(InterAppMessage.FROM_CY3);
				try {
					sendMessage(mapper.writeValueAsString(msg));
				} catch (JsonProcessingException e1) {
					e1.printStackTrace();
				}
			}
			
			@Override
			public void windowIconified(WindowEvent e) {
				// Minimized
				final InterAppMessage msg = InterAppMessage.create()
						.setType(InterAppMessage.TYPE_MINIMIZED)
						.setFrom(InterAppMessage.FROM_CY3);
				try {
					sendMessage(mapper.writeValueAsString(msg));
				} catch (JsonProcessingException ex) {
					ex.printStackTrace();
				}
			}
		});
	}

	@OnWebSocketMessage
	public void onText(Session session, String message) throws IOException {

		// Map message into message object
		final InterAppMessage msg = mapper.readValue(message, InterAppMessage.class);

		final String from = msg.getFrom();

		if (msg.getType().equals(InterAppMessage.TYPE_APP)) {
			appStarted = false;
			final InterAppMessage reply = InterAppMessage.create()
					.setType(InterAppMessage.TYPE_APP)
					.setFrom(InterAppMessage.FROM_CY3)
					.setBody(application);
			
			if(application.contains("save") || application.contains("login")) {
				final JFrame desktop = app.getJFrame();
				desktop.setEnabled(false);
				app.getJMenuBar().setEnabled(false);	
			}
					
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
			pm.kill();
			final JFrame desktop = app.getJFrame();
			desktop.setEnabled(true);
			app.getJMenuBar().setEnabled(true);	
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
			final InterAppMessage reply = new InterAppMessage();
			reply
				.setType(InterAppMessage.TYPE_QUERY)
				.setBody(pm.getQuery())
				.setFrom(InterAppMessage.FROM_CY3)
				.setOptions(cyrestPort);
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
			
			if(!appStarted) {
				appStarted = true;
				return;
			}
			// ignore = true;
			final JFrame desktop = app.getJFrame();
			app.getJMenuBar().setEnabled(true);	
			
			if (desktop.isFocused() && desktop.isActive()) {
			} else {
				EventQueue.invokeLater(new Runnable() {
					@Override
					public void run() {
						desktop.setAlwaysOnTop(true);
						desktop.toFront();
						desktop.repaint();
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						desktop.setAlwaysOnTop(false);
					}
				});

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
			final JFrame desktop = app.getJFrame();
			app.getJMenuBar().setEnabled(true);	
			if (desktop.isFocused() && desktop.isActive()) {
				app.getJFrame().toFront();
			} else {
				app.getJFrame().requestFocus();
				app.getJFrame().toFront();
			}

		} else {
			// Try handlers
			final WSHandler handler = handlers.get(msg.getType());
			if(handler != null) {
				handler.handleMessage(msg, this.currentSession);
			}	
		}
	}
	
	@OnWebSocketConnect
	public void onConnect(Session session) {
		this.currentSession = session;
		latch.countDown();
	}

	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
		currentSession.close();
		currentSession = null;
		// latch.countDown();
		
		
		// Enable window
		final JFrame desktop = app.getJFrame();
		desktop.setEnabled(true);
		app.getJMenuBar().setEnabled(true);	

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
