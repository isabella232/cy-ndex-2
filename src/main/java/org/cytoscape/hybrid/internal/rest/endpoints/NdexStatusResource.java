package org.cytoscape.hybrid.internal.rest.endpoints;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.cytoscape.hybrid.internal.rest.parameter.AppStatusParameters;
import org.cytoscape.hybrid.internal.rest.response.AppStatusResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;


@Api(tags="Apps: CyNDEx-2")
@Path("/cyndex2/v1/status")
public interface NdexStatusResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/")
	@ApiOperation(
			value = "Get current status of the CyNDEx app.",
			notes = "Application status (choose or save) and other properties will be returned.",
			response = AppStatusResponse.class)
	public AppStatusResponse<AppStatusParameters> getAppStatus();
}
