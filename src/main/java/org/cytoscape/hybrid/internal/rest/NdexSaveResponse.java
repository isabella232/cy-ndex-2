package org.cytoscape.hybrid.internal.rest;

public class NdexSaveResponse {
	
	// SUID of the Cytoscape network just saved
	public Long suid;
	
	// UUID of the NDEx entry for the network above
	public String uuid;
	
	public NdexSaveResponse(Long suid, String uuid) {
		this.uuid = uuid;
		this.suid = suid;
	}

}
