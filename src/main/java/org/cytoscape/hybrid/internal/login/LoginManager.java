package org.cytoscape.hybrid.internal.login;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Session-only login information.
 */
public class LoginManager {

	public static final String EVENT_LOGIN = "login";
	
	private Credential login;
	private final PropertyChangeSupport pcs;
	
	public LoginManager() {
		this.pcs = new PropertyChangeSupport(this);
	}
	
	public void setLogin(final Credential login) {
		if (login != null) {
			this.login = login;
		}
		
		pcs.firePropertyChange(EVENT_LOGIN, null, login);
	}

	public Credential getLogin() {
		return login;
	}
	
	public void addPropertyChangeListener(PropertyChangeListener l) {
		pcs.addPropertyChangeListener(l);
	}

}
