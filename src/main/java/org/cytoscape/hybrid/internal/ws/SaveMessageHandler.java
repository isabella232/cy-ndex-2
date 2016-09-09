package org.cytoscape.hybrid.internal.ws;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.JOptionPane;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.hybrid.events.InterAppMessage;
import org.cytoscape.hybrid.events.WSHandler;
import org.cytoscape.hybrid.internal.login.Credential;
import org.cytoscape.hybrid.internal.login.LoginManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.property.CyProperty;
import org.eclipse.jetty.websocket.api.Session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SaveMessageHandler implements WSHandler {

	private final CyApplicationManager appManager;
	private final ObjectMapper mapper;
	
	private final LoginManager manager;
	private final CyRootNetworkManager rootManager;
	private final String cyrestPort;
	
	public SaveMessageHandler(CyApplicationManager appManager, 
			final LoginManager manager, final CyRootNetworkManager rootManager,
			final CyProperty<Properties> props) {
		this.appManager = appManager;
		this.mapper = new ObjectMapper();
		this.manager = manager;
		this.rootManager = rootManager;
		
		this.cyrestPort = props.getProperties().get("rest.port").toString();
	}
	
	
	private void process(final InterAppMessage msg, final Session session) {
		if (msg.getFrom().equals(InterAppMessage.FROM_CY3)) {
			return;
		}

		if (!msg.getType().equals(NdexSaveMessage.TYPE_SAVE)) {
			return;
		}
		
		final Credential credential = manager.getLogin();
		if(credential == null) {
			final InterAppMessage errorReply = InterAppMessage.create()
					.setType(NdexSaveMessage.TYPE_CLOSED)
					.setFrom(InterAppMessage.FROM_CY3);
			try {
				sendMessage(mapper.writeValueAsString(errorReply), session);
			} catch (JsonProcessingException e1) {
				e1.printStackTrace();
			}
			return;
//			throw new IllegalStateException("You have not loggedin yet.");
		}

		// This is the save message from NDEx Save

		final CyNetwork net = appManager.getCurrentNetwork();
		final String networkName = net.getRow(net).get(CyNetwork.NAME, String.class);
		final Long networkSUID = net.getSUID();
		final List<String> propList= new ArrayList<>();
		propList.add(networkSUID.toString());
		propList.add(networkName);
		
		final CyRootNetwork root = rootManager.getRootNetwork(net);
		final String rootNetName = root.getDefaultNetworkTable()
				.getRow(root.getSUID()).get(CyNetwork.NAME, String.class);

		final Map<String, String> saveProps = new HashMap<>();
		saveProps.put(CyNetwork.NAME, networkName);
		saveProps.put(CyNetwork.SUID, networkSUID.toString());
		saveProps.put("root" + CyNetwork.NAME, rootNetName);
		saveProps.put("root" + CyNetwork.SUID, root.getSUID().toString());
		saveProps.put("userName", credential.getUserName());
		saveProps.put("userPass", credential.getUserPass());
		saveProps.put("serverName", credential.getServerName());
		saveProps.put("serverAddress", credential.getServerAddress());
		saveProps.put("cyrestPort", cyrestPort);
		
		final InterAppMessage reply = InterAppMessage.create()
				.setType(NdexSaveMessage.TYPE_SAVE)
				.setFrom(InterAppMessage.FROM_CY3)
				.setOptions(saveProps);
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
		process(msg, session);
	}


	@Override
	public String getType() {
		return NdexSaveMessage.TYPE_SAVE;
	}
}
