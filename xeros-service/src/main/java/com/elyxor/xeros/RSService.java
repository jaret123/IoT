package com.elyxor.xeros;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.util.List;
import java.util.Map;

@Path("/")
@Consumes({"application/json"})
@Produces({"application/json"})
public interface RSService {

	@GET
    @Path("/healthcheck")
	Response healthcheck();    
    
    Response parseCollectionFile(File f, Map<String, String> meta);
    
    @GET
    @Path("/match/{collectionId}")
    Response matchCollection(@PathParam("collectionId") int collectionId );
    
    @GET
    @Path("/unmatch/{collectionId}")
    Response unmatchCollection(@PathParam("collectionId") int collectionId );
    
    @GET
    @Path("/classify/{collectionId}/{classificationId}")
    Response createCollectionClassificationMap(@PathParam("collectionId") int collectionId, @PathParam("classificationId") int classificationId);

    @GET
    @Path("/normalize/{collectionId}")
    Response normalizeCollection(@PathParam("collectionId") int collectionId);
    
    @POST
    @Path("/daiping/{daiIdentifier}")
    Response ping(@PathParam("daiIdentifier") String daiIdentifier);
    
    @GET
    @Path("/pingstatus")
    Response pingStatus();

    @POST
    @Path("/poststatus/{daiIdentifier}/{xerosStatus}/{stdStatus}")
    Response receiveMachineStatus(@PathParam("daiIdentifier") String daiIdentifier, @PathParam("xerosStatus") byte xerosStatus, @PathParam("stdStatus") byte stdStatus);

//    @GET
//    @Path("/machinestatus/{machineId}")
//    Response machineStatus(@PathParam("machineId") int machineId);

    @POST
    @Path("/status/")
    Response getStatus(List<Integer> machineIdList);

    @POST
    @Path("/history/")
    Response getStatusHistory(List<Integer> machineIdList);

    @POST
    @Path("/statusgaps/")
    Response getStatusGaps(List<Integer> machineIdList);

    @GET
    @Path("/statusgaps/")
    Response getStatusGaps();

    @GET
    @Path("/report/")
    @Produces("application/vnd.ms-excel")
    Response getSimpleCycleReport(@Context UriInfo info);

//    @GET
//    @Path("/cycle/{startDate}/{endDate}")
//    @Produces("application/vnd.ms-excel")
//    Response getSimpleCycleReport(@PathParam("startDate") String startDate, @PathParam("endDate") String endDate);
//
//    @GET
//    @Path("/cycle/{startDate}/{endDate}/{exceptionType}")
//    @Produces("application/vnd.ms-excel")
//    Response getSimpleCycleReport(@PathParam("startDate") String startDate, @PathParam("endDate") String endDate, @PathParam("exceptionType") Integer exceptionType);
}
