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
		
		public int hashCode() {
			return username != null ? username.concat("@").concat(url).hashCode() : url.hashCode();
		}
		
		public boolean equals(Object object) {
			if (!(object instanceof ServerKey)) { return false; }
			
			ServerKey serverKey = (ServerKey) object;
			if (serverKey.username == null && this.username == null) {
				return serverKey.url.equals(this.url);
			} else if (serverKey.username == null || this.username == null) {
				return false;
			} else {
				return this.username.equals(serverKey.username) && this.url.equals(serverKey.url);
			}
		}
}
