package org.cytoscape.hybrid.internal.rest.endpoints;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.cytoscape.hybrid.internal.rest.response.AppInfoResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags="Apps: CyNDEx-2")
@Path("/cyndex2/v1")
public interface NdexBaseResource {

	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/")
	@ApiOperation(
			value = "Provide basic information of the CyNDEx-2 app.",
			notes = "App version and other basic information will be provided.",
			response = AppInfoResponse.class)
	public AppInfoResponse getAppInfo();
}
