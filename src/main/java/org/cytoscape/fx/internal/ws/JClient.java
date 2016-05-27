package org.cytoscape.fx.internal.ws;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import javax.swing.JFrame;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.fx.internal.ws.message.InterAppMessage;
import org.eclipse.jetty.websocket.api.Session;
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
	
	public JClient(final CySwingApplication app) {
		this.app = app;
		this.mapper = new ObjectMapper();
		addListener();
	}
	
	private final void addListener() {
		final JFrame desktop = app.getJFrame();
		desktop.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {}
			
			@Override
			public void focusGained(FocusEvent e) {
				System.out.println("**** Focusd *****");
				final InterAppMessage msg = new InterAppMessage();
				msg.setMessage("focued");
				msg.setType(InterAppMessage.CY3);
				try {
					sendMessage(mapper.writeValueAsString(msg));
				} catch (JsonProcessingException e1) {
					e1.printStackTrace();
				}
			}
		});
	}
	
	private CountDownLatch latch= new CountDownLatch(1);

	@OnWebSocketMessage
	public void onText(Session session, String message) throws IOException {
		System.out.println("$$$$$$$$$$$$$$$$$ Message received from server:" + message);
		
		app.getJFrame().setAlwaysOnTop(true);
		app.getJFrame().requestFocus();
		app.getJFrame().setAlwaysOnTop(false);
	}

	@OnWebSocketConnect
	public void onConnect(Session session) {
		System.out.println("Connected to server");
		this.session=session;
		latch.countDown();
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
