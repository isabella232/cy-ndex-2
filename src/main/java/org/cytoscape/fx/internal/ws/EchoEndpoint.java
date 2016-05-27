package org.cytoscape.fx.internal.ws;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.cytoscape.fx.internal.ws.message.InterAppMessage;

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
			
			final InterAppMessage val = mapper.readValue(message, InterAppMessage.class);
			final String from = val.getFrom();
			final String type = val.getType();
			
			// Deligate
			if(from.equals(InterAppMessage.FROM_CY3)) {
				broadcast(message, session);
			} else if(type.equals("connected")) {				
				broadcast(message, session);
			}
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
		System.out.println("* Connected: " + session.getProtocolVersion());
	}


	@OnError
	public void onError(Throwable t) {
		t.printStackTrace();
	}

	@OnClose
	public void onClose(CloseReason reason, Session session) {
		System.out.println("* Disconnected: " + session.getProtocolVersion());
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