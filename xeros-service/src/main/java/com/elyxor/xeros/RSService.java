package com.elyxor.xeros;

import java.io.File;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

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
    @Path("/machinestatus/{daiIdentifier}/{xerosStatus}/{stdStatus}")
    Response receiveMachineStatus(@PathParam("daiIdentifier") String daiIdentifier, @PathParam("xerosStatus") byte xerosStatus, @PathParam("stdStatus") byte stdStatus);
}
