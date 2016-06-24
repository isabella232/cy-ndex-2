package org.cytoscape.hybrid.internal.login;

/**
 * Session-only login information.
 */
public class LoginManager {

	private Credential login;

	public void setLogin(final Credential login) {
		if (login != null) {
			this.login = login;
		}
	}

	public Credential getLogin() {
		return login;
	}

}
