package org.cytoscape.hybrid.internal.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/ndex/v1/networks")
public interface NdexImportResource {

	@GET
	@Path("/current")
	@Produces(MediaType.APPLICATION_JSON)
	public SummaryResponse getCurrentNetworkSummary();

	@POST
	@Produces("application/json")
	@Consumes("application/json")
	@Path("/")
	@ApiOperation(value = "Import network from NDEx", notes = "<br><br>Import from NDEx", response = NdexImportResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = "Network does not exist", response = NdexImportResponse.class), })
	public NdexResponse<NdexImportResponse> createNetworkFromNdex(NdexImportParams params);
	
	@POST
	@Produces("application/json")
	@Consumes("application/json")
	@Path("/current")
	@ApiOperation(value = "Save current network to NDEx", notes = "<br><br>Save to NDEx", response = NdexImportResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = "Network does not exist", response = NdexSaveResponse.class), })
	public NdexResponse<NdexSaveResponse> saveCurrentNetworkToNdex(final NdexSaveParams params);
	
	@POST
	@Produces("application/json")
	@Consumes("application/json")
	@Path("/{suid}")
	@ApiOperation(value = "Save network to NDEx", notes = "<br><br>Save to NDEx", response = NdexSaveResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = "Network does not exist", response = NdexImportResponse.class), })
	public NdexResponse<NdexSaveResponse> saveNetworkToNdex(@PathParam("suid") Long suid, final NdexSaveParams params);
	
	@PUT
	@Produces("application/json")
	@Consumes("application/json")
	@Path("/{suid}")
	@ApiOperation(value = "Save network to NDEx", notes = "<br><br>Save to NDEx", response = NdexSaveResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = "Network does not exist", response = NdexImportResponse.class), })
	public NdexResponse<NdexSaveResponse> updateNetworkInNdex(@PathParam("suid") Long suid, final NdexSaveParams params);
	
	@PUT
	@Produces("application/json")
	@Consumes("application/json")
	@Path("/current")
	@ApiOperation(value = "Save current network to NDEx", notes = "<br><br>Save to NDEx", response = NdexImportResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = "Network does not exist", response = NdexSaveResponse.class), })
	public NdexResponse<NdexSaveResponse> updateCurrentNetworkInNdex(final NdexSaveParams params);
}
