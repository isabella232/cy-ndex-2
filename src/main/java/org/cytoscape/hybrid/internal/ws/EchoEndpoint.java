package org.cytoscape.hybrid.internal.ws;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.cytoscape.hybrid.events.InterAppMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Basic endpoint for passing messages between the two apps.
 * 
 */
@ServerEndpoint(value = "/echo")
public class EchoEndpoint {
	
	private final ObjectMapper mapper;
	
	public EchoEndpoint() {
		this.mapper = new ObjectMapper();
	}
	
	@OnMessage
	public void onMessage(String message, Session session) {
		try {
			System.out.println("On Message: " + message);
			System.out.println("On Message: Sesseion = " + session.getId());
			
			final InterAppMessage val = mapper.readValue(message, InterAppMessage.class);
			final String from = val.getFrom();
			final String type = val.getType();
			
			broadcast(message, session);
		} catch(Exception e) {
			System.out.println("Invalid msg: " + message);
		}
	}
	
	private final void broadcast(String msg, Session session) {
		session.getOpenSessions().forEach(s->{
			try {
				s.getBasicRemote().sendText(msg);
			} catch (Exception e) {
			}
		});
	}

	@OnOpen
	public void onOpen(Session session) {
		System.out.println("* Client Connected: " + session.getId());
	}


	@OnError
	public void onError(Throwable t) {
		t.printStackTrace();
	}

	@OnClose
	public void onClose(CloseReason reason, Session session) {
		System.out.println("* Disconnected: " + session.getId());
		// Client disconnected:
		final InterAppMessage msg = new InterAppMessage();
		msg.setFrom(InterAppMessage.FROM_CY3);
		msg.setType(InterAppMessage.TYPE_CLOSED);
		
		try {
			broadcast(mapper.writeValueAsString(msg), session);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}