package org.cytoscape.cyndex2.internal.rest.endpoints;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.cytoscape.ci.model.CIResponse;
import org.cytoscape.cyndex2.internal.rest.parameter.AppStatusParameters;
import org.cytoscape.cyndex2.internal.rest.response.AppStatusResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;


@Api(tags="Apps: CyNDEx-2")
@Path("/cyndex2/v1/status")
public interface NdexStatusResource {

	public final static String NDEX_UUID_TAG = "ndex.uuid";
	public static final String SINGLETON_COLUMN_NAME = "ndex.createdFromSingleton";
	
	@ApiModel(
			value="App Status Response",
			parent=CIResponse.class)
    public static class CIAppStatusResponse extends CIResponse<AppStatusResponse<AppStatusParameters>>{
    }
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/")
	@ApiOperation(
			value = "Get current status of the CyNDEx app.",
			notes = "Application status (choose or save) and other properties will be returned.",
			response = CIAppStatusResponse.class)
	public CIAppStatusResponse getAppStatus();
}
