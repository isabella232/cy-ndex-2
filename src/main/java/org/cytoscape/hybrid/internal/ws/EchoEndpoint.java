package org.cytoscape.hybrid.internal.ws;

import java.io.IOException;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.cytoscape.hybrid.events.InterAppMessage;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
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
		
		InterAppMessage value = null;
		try {
			value = mapper.readValue(message, InterAppMessage.class);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		// Ignore the message is just ping.
		if(value != null && value.getType().equals(InterAppMessage.TYPE_ALIVE)) {
			return;
		}
		
		try {
			System.out.println("On Message: " + message);
			broadcast(message, session);
		} catch(Exception e) {
			System.out.println("Invalid msg: " + message);
		}
	}
	
	private final void broadcast(String msg, Session session) {
		session.getOpenSessions().forEach(s->{
				if(session != s && s.isOpen()) {
					try {
						s.getBasicRemote().sendText(msg);
					} catch (Exception e) {
						e.printStackTrace();
					}
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
		System.out.println("* Client Disconnected: " + session.getId());
		
		final InterAppMessage msg = new InterAppMessage();
		msg.setFrom(InterAppMessage.FROM_CY3)
			.setType(InterAppMessage.TYPE_CLOSED);
		
		try {
			broadcast(mapper.writeValueAsString(msg), session);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			session.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}