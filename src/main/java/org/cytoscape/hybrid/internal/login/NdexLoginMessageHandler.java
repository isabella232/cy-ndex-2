package org.cytoscape.hybrid.internal.login;

import java.io.IOException;

import javax.management.OperationsException;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.hybrid.events.InterAppMessage;
import org.cytoscape.hybrid.events.WSHandler;
import org.eclipse.jetty.websocket.api.Session;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class NdexLoginMessageHandler implements WSHandler {

	private final CyApplicationManager appManager;
	private final ObjectMapper mapper;
	
	private final LoginManager loginManager;

	public NdexLoginMessageHandler(final CyApplicationManager appManager, final LoginManager loginManager) {
		this.appManager = appManager;
		this.mapper = new ObjectMapper();
		mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

		this.loginManager = loginManager;
	}

	private void process(final InterAppMessage msg, final Session session) throws JsonParseException, JsonMappingException, IOException {
		if (msg.getFrom().equals(InterAppMessage.FROM_CY3)) {
			return;
		}

		if (!msg.getType().equals(NdexLoginMessage.TYPE_LOGIN)) {
			return;
		}

		// Save login
		final Object options = msg.getOptions();
		System.out.println("** Got LOGIN Event: " + msg);
		
		final Credential credential = mapper.readValue(mapper.writeValueAsString(options), Credential.class);
		
		loginManager.setLogin(credential);
		System.out.println("** \n\nOK!: " + credential);
		System.out.println(credential.getServerName());
		System.out.println(credential.getServerAddress());
		System.out.println(credential.getUserName());
		System.out.println(credential.getUserPass());
		System.out.println(credential.getLoggedIn());
	}


	@Override
	public void handleMessage(InterAppMessage msg, Session session) {
		System.out.println("** Login Handler Event: " + msg);
		try {
			process(msg, session);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getType() {
		return NdexLoginMessage.TYPE_LOGIN;
	}
}
