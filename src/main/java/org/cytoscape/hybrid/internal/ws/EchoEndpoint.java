package org.cytoscape.hybrid.internal.ws;

import java.io.IOException;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.cytoscape.hybrid.events.InterAppMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Basic endpoint for passing messages between the two apps.
 * 
 */
@ServerEndpoint(value = "/echo")
public class EchoEndpoint {

	private final Logger logger = LoggerFactory.getLogger(EchoEndpoint.class);
	
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
			broadcast(message, session);
		} catch(Exception e) {
			logger.warn("Invalid message: " + message, e);
		}
	}
	
	private final void broadcast(String msg, Session session) {
		session.getOpenSessions().forEach(s->{
				if(session != s && s.isOpen()) {
					try {
						final Basic remote = s.getBasicRemote();
						remote.sendText(msg);
					} catch (Exception e) {
						logger.warn("Tried to open multiple instances.", e);
					}
				}
		});
	}

	@OnOpen
	public void onOpen(Session session) {
		logger.info("Client Connected: " + session.getId());
	}


	@OnError
	public void onError(Throwable t) {
		t.printStackTrace();
	}

	@OnClose
	public void onClose(CloseReason reason, Session session) {
		logger.info("Client Disconnected: " + session.getId());
		
		final InterAppMessage msg = new InterAppMessage();
		msg.setFrom(InterAppMessage.FROM_CY3)
			.setType(InterAppMessage.TYPE_CLOSED);
		
		try {
			broadcast(mapper.writeValueAsString(msg), session);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			logger.warn("Could not encode message: ", e);
		}
		try {
			session.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}