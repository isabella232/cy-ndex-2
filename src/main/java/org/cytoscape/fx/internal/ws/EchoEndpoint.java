package org.cytoscape.fx.internal.ws;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.cytoscape.fx.internal.ws.message.InterAppMessage;

import com.fasterxml.jackson.databind.ObjectMapper;

@ServerEndpoint(value = "/echo")
public class EchoEndpoint {
	
	private final ObjectMapper mapper;
	
	public EchoEndpoint() {
		this.mapper = new ObjectMapper();
	}
	
	@OnMessage
	public void onMessage(String message, Session session) {
		try {
			System.out.println(message);
			
			final InterAppMessage val = mapper.readValue(message, InterAppMessage.class);
			final String type = val.getType();
			if(type.equals(InterAppMessage.CY3)) {
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
		System.out.println("Total sessions: " + session.getOpenSessions().size());
	}


	@OnError
	public void onError(Throwable t) {
		t.printStackTrace();
	}

	@OnClose
	public void onClose(Session session) {
		System.out.println("onClose");
	}
}