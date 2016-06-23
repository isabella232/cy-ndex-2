package org.cytoscape.hybrid.internal.login;

/**
 * Session-only login information.
 */
public class LoginManager {

	private Credential login;

	public void setLogin(Credential login) {
		if (login != null) {
			this.login = login;
		}
	}

	public Credential getLogin() {
		if (login == null) {
			throw new IllegalStateException("Login information is missing.");
		}

		return login;
	}

}
