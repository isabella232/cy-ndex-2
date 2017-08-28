package org.cytoscape.cyndex2.internal.util;

import java.net.InetSocketAddress;
import java.net.Socket;

public class NetworkConnectionTester {

	private static final int PORT_NUMBER = 80;
	
	public static boolean isReacheable(final String host) {
		
		try {
			Socket sock = new Socket();
			sock.connect(new InetSocketAddress(host, PORT_NUMBER));
			sock.close();
		} catch(Exception e) {
			return false;
		}
		
		return true;
	}
}
