package org.cytoscape.hybrid.internal.ws;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.hybrid.events.InterAppMessage;
import org.cytoscape.hybrid.events.WSHandler;
import org.cytoscape.hybrid.events.WebSocketEvent;
import org.cytoscape.hybrid.events.WebSocketEventListener;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.eclipse.jetty.websocket.api.Session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SaveMessageHandler implements WSHandler {

	private final CyApplicationManager appManager;
	private final ObjectMapper mapper;
	
	public SaveMessageHandler(CyApplicationManager appManager) {
		this.appManager = appManager;
		this.mapper = new ObjectMapper();
	}
	
	
	private void process(final InterAppMessage msg, final Session session) {
		if (msg.getFrom().equals(InterAppMessage.FROM_CY3)) {
			return;
		}

		if (!msg.getType().equals(NdexSaveMessage.TYPE_SAVE)) {
			return;
		}

		// This is the save message from NDEx Save

		System.out.println("** Got SAVE Event: " + msg);
		final CyNetwork net = appManager.getCurrentNetwork();
		final String networkName = net.getRow(net).get(CyNetwork.NAME, String.class);
		final Long networkSUID = net.getSUID();
		final List<String> propList= new ArrayList<>();
		propList.add(networkSUID.toString());
		propList.add(networkName);
		
		final InterAppMessage reply = InterAppMessage.create()
				.setType(NdexSaveMessage.TYPE_SAVE)
				.setFrom(InterAppMessage.FROM_CY3)
				.setBody(propList.toString());
		try {
			sendMessage(mapper.writeValueAsString(reply), session);
		} catch (JsonProcessingException e1) {
			e1.printStackTrace();
		}
	}
	
	private void sendMessage(final String str, final Session session) {
		if (session == null || session.isOpen() == false) {
			return;
		}

		try {
			session.getRemote().sendString(str);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	@Override
	public void handleMessage(InterAppMessage msg, Session session) {
		System.out.println("** Save Handler Event: " + msg);
		process(msg, session);
	}


	@Override
	public String getType() {
		return NdexSaveMessage.TYPE_SAVE;
	}
}
