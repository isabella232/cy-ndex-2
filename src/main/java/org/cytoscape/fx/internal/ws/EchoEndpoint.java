package org.cytoscape.fx.internal.ws;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/echo")
public class EchoEndpoint {
	@OnMessage
	public String onMessage(String message, Session session) {
		return message;
	}

	@OnOpen
	public void onOpen(Session session) {
		System.out.println("onOpen");
	}

	@OnMessage
	public String onMessage(String message) {
		System.out.println("onMessage " + message);
		return "You said \"" + message + "\".";
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