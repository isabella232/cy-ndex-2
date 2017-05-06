package org.cytoscape.hybrid.internal.rest.endpoints;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;


@Api(tags="Apps: CyNDEx-2")
@Path("/cyndex2/v1/status")
public interface NdexStatusResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/")
	@ApiOperation(
			value = "Get current state of the app.",
			notes = "Application state (choose or save) and other properties will be returned.",
			response = Map.class)
	public Map<String, Object> getAppStatus();
}
