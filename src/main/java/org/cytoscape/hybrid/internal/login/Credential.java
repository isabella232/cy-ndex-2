package org.cytoscape.hybrid.internal.login;

public class Credential {

	private String id;
	private String pw;
	private ServiceInfo serviceInfo;

	public static Credential create() {
		return new Credential();
	}

	public String getId() {
		return id;
	}

	public Credential setId(String id) {
		this.id = id;
		return this;
	}

	public String getPw() {
		return pw;
	}

	public Credential setPw(String pw) {
		this.pw = pw;
		return this;
	}

	public ServiceInfo getServiceInfo() {
		return serviceInfo;
	}

	public Credential setServiceInfo(ServiceInfo serviceInfo) {
		this.serviceInfo = serviceInfo;
		return this;
	}

}
