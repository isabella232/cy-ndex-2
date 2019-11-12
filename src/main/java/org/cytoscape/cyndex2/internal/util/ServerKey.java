package org.cytoscape.cyndex2.internal.util;

public class ServerKey {
		
		public String username;
		public String url;
	
		public ServerKey() {}
		
		public ServerKey(String username, String url) {
			this.username = username;
			this.url = url;
		}
		
		public ServerKey(Server server) {
			this.username = server.getUsername();
			this.url = server.getUrl();
		}
}
