package org.cytoscape.cyndex2.internal.rest.endpoints;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.cytoscape.ci.model.CIResponse;
import org.cytoscape.cyndex2.internal.rest.response.AppInfoResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;

@Api(tags="Apps: CyNDEx-2")
@Path("/cyndex2/v1")
public interface NdexBaseResource {

	@ApiModel(
			value="App Info Response",
			parent=CIResponse.class)
    public static class CIAppInfoResponse extends CIResponse<AppInfoResponse>{
    }
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/")
	@ApiOperation(
			value = "Provide basic information of the CyNDEx-2 app.",
			notes = "App version and other basic information will be provided.",
			response = CIAppInfoResponse.class)
	public CIAppInfoResponse getAppInfo();
}
