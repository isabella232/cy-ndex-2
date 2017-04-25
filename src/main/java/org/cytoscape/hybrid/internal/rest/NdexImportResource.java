package org.cytoscape.hybrid.internal.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
	@Produces(MediaType.APPLICATION_JSON)
	public NdexImportResponse importNetwork(@PathParam("uuid") String uuid);

	@POST
	@Produces("application/json")
	@Consumes("application/json")
	@Path("/")
	@ApiOperation(value = "Import network from NDEx", notes = "<br><br>Import from NDEx", response = NdexImportResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = "Network does not exist", response = NdexImportResponse.class), })
	public NdexImportResponse createNetworkFromNdex(NdexImportParams params);
	
	@POST
	@Produces("application/json")
	@Consumes("application/json")
	@Path("/current")
	@ApiOperation(value = "Save current network to NDEx", notes = "<br><br>Save to NDEx", response = NdexImportResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = "Network does not exist", response = NdexSaveResponse.class), })
	public NdexSaveResponse saveCurrentNetworkToNdex(final NdexImportParams params);
	
	@POST
	@Produces("application/json")
	@Consumes("application/json")
	@Path("/{suid}")
	@ApiOperation(value = "Save network to NDEx", notes = "<br><br>Save to NDEx", response = NdexSaveResponse.class)
	@ApiResponses(value = {
			@ApiResponse(code = 404, message = "Network does not exist", response = NdexImportResponse.class), })
	public NdexSaveResponse saveNetworkToNdex(@PathParam("suid") Long suid, final NdexImportParams params);
}
