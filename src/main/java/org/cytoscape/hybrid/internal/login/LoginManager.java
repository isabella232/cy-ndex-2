package org.cytoscape.hybrid.internal.login;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;

/**
 * Session-only login information.
 */
public class LoginManager {

	private static final Credential DEFAULT_SERVER = 
			Credential.create()
				.setServerAddress("http://public.ndexbio.org/")
				.setServerName("NDEx Public Server");

	public static final String EVENT_LOGIN = "login";
	private final PropertyChangeSupport pcs;

	private Credential currentServer = null;
	private Map<String, Credential> servers;

	public LoginManager() {
		this.servers = new HashMap<>();
		this.servers.put(DEFAULT_SERVER.getServerName(), DEFAULT_SERVER);
		this.pcs = new PropertyChangeSupport(this);
	}

	public void setLogin(final Credential login) {
		this.currentServer = login;
		this.getServers().put(login.getServerName(), login);
		pcs.firePropertyChange(EVENT_LOGIN, "last", login);
	}

	public Credential getLogin() {
		return currentServer;
	}

	public Map<String, Credential> getServers() {
		return servers;
	}

	public void setServers(Map<String, Credential> servers) {
		this.servers = servers;
	}

	public void addPropertyChangeListener(PropertyChangeListener l) {
		pcs.addPropertyChangeListener(l);
	}

}
