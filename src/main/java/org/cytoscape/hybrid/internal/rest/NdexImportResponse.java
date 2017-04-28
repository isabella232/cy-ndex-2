package org.cytoscape.hybrid.internal.rest;

public final class NdexImportResponse {

	private final Long suid;
	private final String uuid;

	public NdexImportResponse(final Long suid, final String uuid) {
		this.suid = suid;
		this.uuid = uuid;
	}

	public String getUuid() {
		return uuid;
	}

	public Long getSuid() {
		return suid;
	}

}
