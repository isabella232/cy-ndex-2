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
import javax.swing.SwingUtilities;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.hybrid.events.InterAppMessage;
import org.cytoscape.hybrid.events.WSHandler;
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

	private static final InterAppMessage ALIVE;

	private final Map<String, WSHandler> handlers;
	
	private boolean appStarted = false;
	
	private boolean selfEvent = false;
	
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
			ExternalAppManager pm, 
			final LoginManager loginManager, final CyProperty<Properties> props) {
		this.app = app;
		this.pm = pm;
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
		
		desktop.setAutoRequestFocus(false);
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
				if(selfEvent) {
					selfEvent = false;
					return;
				}
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
			
			@Override
			public void windowDeiconified(WindowEvent e) {
				if(selfEvent) {
					selfEvent = false;
					return;
				}
				// Window size restored
				final InterAppMessage msg = InterAppMessage.create()
						.setType(InterAppMessage.TYPE_RESTORED)
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

		System.out.println("GOT MSG: " + message);
		
		// Map message into message object
		final InterAppMessage msg = mapper.readValue(message, InterAppMessage.class);

		final String from = msg.getFrom();

		if (msg.getType().equals(InterAppMessage.TYPE_APP)) {
			appStarted = false;
			final InterAppMessage reply = InterAppMessage.create()
					.setType(InterAppMessage.TYPE_APP)
					.setFrom(InterAppMessage.FROM_CY3)
					.setBody(application);
			
			// Make the window disabled.
			final JFrame desktop = app.getJFrame();
			
			desktop.setEnabled(false);
			app.getJToolBar().setEnabled(false);	
			desktop.setFocusable(false);
					
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
			
			EventQueue.invokeLater(new Runnable() {

				@Override
				public void run() {
					final JFrame desktop = app.getJFrame();
					desktop.setEnabled(true);
					desktop.setFocusable(true);
					desktop.setAlwaysOnTop(true);
					desktop.requestFocus();
					desktop.toFront();
					desktop.repaint();
					try {
						Thread.sleep(40);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					desktop.setAlwaysOnTop(false);
					app.getJToolBar().setEnabled(true);
				}
			});
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
		} else if (msg.getType().equals(InterAppMessage.TYPE_FOCUS)) {
			if (from.equals(InterAppMessage.FROM_CY3)) {
				return;
			}
			final JFrame desktop = app.getJFrame();
			
			if(!appStarted) {
				appStarted = true;
				return;
			}
			
			if (desktop.isFocused()) {
			} else {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						desktop.setAlwaysOnTop(true);
						desktop.toFront();
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						desktop.repaint();
						desktop.setAlwaysOnTop(false);
					}
				});
			}
		} else if (msg.getType().equals(InterAppMessage.TYPE_MINIMIZED)) {
			if (from.equals(InterAppMessage.FROM_CY3)) {
				return;
			}
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					final JFrame desktop = app.getJFrame();
					
					synchronized (desktop) {
						if (desktop.getState() == JFrame.NORMAL) {
							selfEvent = true;
							desktop.setState(JFrame.ICONIFIED);
						}
					}
				}
			});
			
		} else if (msg.getType().equals(InterAppMessage.TYPE_RESTORED)) {
			if (from.equals(InterAppMessage.FROM_CY3)) {
				return;
			}
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					final JFrame desktop = app.getJFrame();
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					synchronized (desktop) {
						if (desktop.getState() == JFrame.ICONIFIED) {
							selfEvent = true;
							desktop.setState(JFrame.NORMAL);
						}
					}
				}
			});
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
		app.getJToolBar().setEnabled(true);	
		
		System.out.println("--------------------- SOK closed");;
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
