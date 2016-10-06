package org.cytoscape.hybrid.internal.login;

public class Credential {

	private String userName;
	private String userPass;
	private String serverName;
	private String serverAddress;
	private Boolean loggedIn;
	private String serverVersion;

	public static Credential create() {
		return new Credential();
	}

	public String getUserName() {
		return userName;
	}

	public Credential setUserName(String userName) {
		this.userName = userName;
		return this;
	}

	public String getUserPass() {
		return userPass;
	}

	public Credential setUserPass(String userPass) {
		this.userPass = userPass;
		return this;
	}

	public Boolean getLoggedIn() {
		return loggedIn;
	}

	public void setLoggedIn(Boolean loggedIn) {
		this.loggedIn = loggedIn;
	}

	public String getServerName() {
		return serverName;
	}

	public Credential setServerName(String serverName) {
		this.serverName = serverName;
		return this;
	}

	public String getServerAddress() {
		return serverAddress;
	}

	public Credential setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
		return this;
	}

	public String getServerVersion() {
		return serverVersion;
	}

	public void setServerVersion(String serverVersion) {
		this.serverVersion = serverVersion;
	}
}
