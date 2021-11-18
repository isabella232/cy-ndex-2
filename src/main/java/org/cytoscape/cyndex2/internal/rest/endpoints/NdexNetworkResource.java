package org.cytoscape.cyndex2.internal.rest.endpoints;

import java.io.InputStream;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.cytoscape.ci.model.CIResponse;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExBasicSaveParameters;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExImportParameters;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExSaveParameters;
import org.cytoscape.cyndex2.internal.rest.response.NdexBaseResponse;
import org.cytoscape.cyndex2.internal.rest.response.SummaryResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;


@Api(tags="Apps: CyNDEx-2")
@Path("/cyndex2/v1/networks")
public interface NdexNetworkResource {
	
	
	@ApiModel(
			value="Summary Response",
			parent=CIResponse.class)
    public static class CISummaryResponse extends CIResponse<SummaryResponse>{/**/
    }

	@ApiOperation(
			value = "Get the summary of current network and collection.",
			notes = "Returns summary of collection contains current network.",
			response = CISummaryResponse.class)
	@GET
	@Path("/current")
	@Produces(MediaType.APPLICATION_JSON)
	public CISummaryResponse getCurrentNetworkSummary();
	
	@ApiOperation(
			value = "Get the summary of specified network and collection.",
			notes = "Returns summary of collection containing the specified network.",
			response = CISummaryResponse.class)
	@GET
	@Path("/{suid}")
	@Produces(MediaType.APPLICATION_JSON)
	public CISummaryResponse getNetworkSummary(
			@ApiParam(value="Cytoscape Collection/Subnetwork SUID") @PathParam("suid")final Long suid);


	@ApiModel(
			value="NDEx Base Response",
			parent=CIResponse.class)
    public static class CINdexBaseResponse extends CIResponse<NdexBaseResponse>{/**/}
	
	@POST
	@Produces("application/json")
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/")
	@ApiOperation(
			value = "Import network from NDEx",
			notes = "Import network(s) from NDEx.",
			response = CINdexBaseResponse.class)
	@ApiResponses(
			value = {
						@ApiResponse(code = 404, message = "Network does not exist", response = CINdexBaseResponse.class)
					}
			)
	public CINdexBaseResponse createNetworkFromNdex(
			 @ApiParam(value = "Raw CX object to be imported to Cytoscape.", required = true) NDExImportParameters params);
	

	@POST
	@Produces("application/json")
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/cx")
	@ApiOperation(
			value = "Import network(s) from cyRestClient",
			notes = "Import network(s) from cyRestClient.",
			response = CINdexBaseResponse.class)
	@ApiImplicitParams(
			@ApiImplicitParam(value="CX network",  paramType="body", required=true)
			)	
/*	@ApiResponses(
			value = {
						@ApiResponse(code = 404, message = "Network does not exist", response = CINdexBaseResponse.class)
					}
			) */
	public CINdexBaseResponse createNetworkFromCx(
			@ApiParam(hidden=true) final InputStream is
			/* @Context HttpServletRequest request /*, byte[] input*//*NdexImportParams params*/);

	
	@POST
	@Produces("application/json")
	@Consumes("application/json")
	@Path("/current")
	@ApiOperation(value = "Save current network/collection to NDEx", notes = "Save current network/collection to NDEx", response = CINdexBaseResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = "Current network does not exist", response = CINdexBaseResponse.class), })
	public CINdexBaseResponse saveCurrentNetworkToNdex(
			@ApiParam(value = "Properties required to save current network to NDEx.", required = true) final NDExSaveParameters params);
	
	@POST
	@Produces("application/json")
	@Consumes("application/json")
	@Path("/{suid}")
	@ApiOperation(value = "Save network/collection to NDEx", notes = "Save a network/collection to NDEx", response = CINdexBaseResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = "Network does not exist", response = CINdexBaseResponse.class), })
	public CINdexBaseResponse saveNetworkToNdex(
			@ApiParam(value="Cytoscape Collection/Subnetwork SUID") @PathParam("suid") Long suid,
			@ApiParam(value = "Properties required to save network to NDEx.", required = true) final NDExSaveParameters params);

	
	@PUT
	@Produces("application/json")
	@Consumes("application/json")
	@Path("/{suid}")
	@ApiOperation(value = "Update an existing NDEx network entry", notes = "Update an NDEx network.", response = CINdexBaseResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = "Network does not exist", response = CINdexBaseResponse.class), })
	public CINdexBaseResponse updateNetworkInNdex(
			@ApiParam(value="Cytoscape Collection/Subnetwork SUID") @PathParam("suid") Long suid,
			@ApiParam(value = "Properties required to update a network record in NDEx.", required = true) final NDExBasicSaveParameters params);

	
	@PUT
	@Produces("application/json")
	@Consumes("application/json")
	@Path("/{suid}/NDEXUUID")
	@ApiOperation(value = "Set the associcated NDEx UUID in the specified network", notes = "Set NDEx UUID in a network.", response = CINdexBaseResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = "Network does not exist", response = CINdexBaseResponse.class), })
	public CINdexBaseResponse updateNdexUUIDOfNetwork(
			@ApiParam(value="Cytoscape Collection/Subnetwork SUID") @PathParam("suid") Long suid,
			@ApiParam(value = "Properties required to update a network record in NDEx.", required = true) final Map<String,String> rec);
	
	
	@PUT
	@Produces("application/json")
	@Consumes("application/json")
	@Path("/current")
	@ApiOperation(
			value = "Update current Cytoscape network record in NDEx", 
			notes = "Update current network's record in NDEx", 
			response = CINdexBaseResponse.class)
	@ApiResponses(
			value = {
					@ApiResponse(code = 404, message = "Network does not exist", response = CINdexBaseResponse.class), })
	public CINdexBaseResponse updateCurrentNetworkInNdex(
			@ApiParam(value = "Properties required to update a network record in NDEx.", required = true) final NDExBasicSaveParameters params);
}
