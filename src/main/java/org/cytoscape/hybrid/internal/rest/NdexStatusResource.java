package org.cytoscape.hybrid.internal.rest;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/ndex/v1")
public interface NdexStatusResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String, String> status();
}
