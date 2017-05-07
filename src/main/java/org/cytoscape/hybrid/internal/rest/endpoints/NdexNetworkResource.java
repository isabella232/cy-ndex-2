package org.cytoscape.hybrid.internal.rest.endpoints;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.cytoscape.hybrid.internal.rest.parameter.NdexImportParams;
import org.cytoscape.hybrid.internal.rest.parameter.NdexSaveParameters;
import org.cytoscape.hybrid.internal.rest.response.NdexBaseResponse;
import org.cytoscape.hybrid.internal.rest.response.SummaryResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;


@Api(tags="Apps: CyNDEx-2")
@Path("/cyndex2/v1/networks")
public interface NdexNetworkResource {

	@ApiOperation(
			value = "Get the summary of current network and collection.",
			notes = "Returns summary of collection contains current network.",
			response = SummaryResponse.class)
	@GET
	@Path("/current")
	@Produces(MediaType.APPLICATION_JSON)
	public SummaryResponse getCurrentNetworkSummary();


	@POST
	@Produces("application/json")
	@Consumes("application/json")
	@Path("/")
	@ApiOperation(
			value = "Import network from NDEx",
			notes = "Import network(s) from NDEx.",
			response = NdexBaseResponse.class)
	@ApiResponses(
			value = {
						@ApiResponse(code = 404, message = "Network does not exist", response = NdexBaseResponse.class)
					}
			)
	public NdexBaseResponse createNetworkFromNdex(
			 @ApiParam(value = "Properties required to import network from NDEx.", required = true) NdexImportParams params);
	
	@POST
	@Produces("application/json")
	@Consumes("application/json")
	@Path("/current")
	@ApiOperation(value = "Save current network to NDEx", notes = "Save current network to NDEx", response = NdexBaseResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = "Current network does not exist", response = NdexBaseResponse.class), })
	public NdexBaseResponse saveCurrentNetworkToNdex(
			@ApiParam(value = "Properties required to save current network to NDEx.", required = true) final NdexSaveParameters params);
	
	@POST
	@Produces("application/json")
	@Consumes("application/json")
	@Path("/{suid}")
	@ApiOperation(value = "Save network collection to NDEx", notes = "Save a collection to NDEx", response = NdexBaseResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = "Network does not exist", response = NdexBaseResponse.class), })
	public NdexBaseResponse saveNetworkToNdex(
			@PathParam("suid") Long suid, 
			@ApiParam(value = "Properties required to save network to NDEx.", required = true) final NdexSaveParameters params);
	
	@PUT
	@Produces("application/json")
	@Consumes("application/json")
	@Path("/{suid}")
	@ApiOperation(value = "Update an existing NDEx network entry", notes = "Update an NDEx network.", response = NdexBaseResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = "Network does not exist", response = NdexBaseResponse.class), })
	public NdexBaseResponse updateNetworkInNdex(
			@PathParam("suid") Long suid,
			@ApiParam(value = "Properties required to update a network record in NDEx.", required = true) final NdexSaveParameters params);
	
	@PUT
	@Produces("application/json")
	@Consumes("application/json")
	@Path("/current")
	@ApiOperation(
			value = "Update current Cytoscape network record in NDEx", 
			notes = "Update current netwprk's record in NDEx", 
			response = NdexBaseResponse.class)
	@ApiResponses(
			value = {
					@ApiResponse(code = 404, message = "Network does not exist", response = NdexBaseResponse.class), })
	public NdexBaseResponse updateCurrentNetworkInNdex(
			@ApiParam(value = "Properties required to update a network record in NDEx.", required = true) final NdexSaveParameters params);
}
