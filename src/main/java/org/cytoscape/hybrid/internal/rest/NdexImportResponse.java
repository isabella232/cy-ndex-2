package org.cytoscape.hybrid.internal.rest;

public class NdexImportResponse extends NdexResponse {

	public Long suid;
	public String uuid;

	public NdexImportResponse() {
	}

	public NdexImportResponse(Long suid, String uuid) {
		this.suid = suid;
		this.uuid = uuid;
	}

}
