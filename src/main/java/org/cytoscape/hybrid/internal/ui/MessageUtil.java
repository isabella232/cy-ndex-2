package org.cytoscape.hybrid.internal.ui;

import org.cytoscape.hybrid.events.InterAppMessage;
import org.cytoscape.hybrid.internal.ws.WSClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MessageUtil {

	private static final ObjectMapper mapper = new ObjectMapper();
	
	
	public static void reauestExternalAppFocus(final WSClient client) {

		final InterAppMessage reply = new InterAppMessage();
		reply.setType(InterAppMessage.TYPE_FOCUS_SUCCESS).setFrom(InterAppMessage.FROM_CY3);
		try {
			client.getSocket().sendMessage(mapper.writeValueAsString(reply));
		} catch (JsonProcessingException e1) {
			e1.printStackTrace();
		}
	}

}
