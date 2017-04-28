package org.cytoscape.hybrid.internal.rest;

public final class CIError {

	private final Integer status;
	private final String code;
	private final String message;
	private final String link;


	public CIError(Integer status, String code, String message, String link) {
		this.status = status;
		this.code = code;
		this.message = message;
		this.link = link;
	}

	public Integer getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	public String getLink() {
		return link;
	}
}
