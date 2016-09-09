package org.cytoscape.hybrid.internal.login;

import java.io.IOException;

import org.cytoscape.hybrid.events.InterAppMessage;
import org.cytoscape.hybrid.events.WSHandler;
import org.eclipse.jetty.websocket.api.Session;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class NdexLoginMessageHandler implements WSHandler {

	private final ObjectMapper mapper;
	
	private final LoginManager loginManager;

	public NdexLoginMessageHandler(final LoginManager loginManager) {
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
		final Credential credential = mapper.readValue(mapper.writeValueAsString(options), Credential.class);
		
		loginManager.setLogin(credential);
	}


	@Override
	public void handleMessage(InterAppMessage msg, Session session) {
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
