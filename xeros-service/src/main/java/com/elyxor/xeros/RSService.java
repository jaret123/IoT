package com.elyxor.xeros;

import java.io.File;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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
    
    Response parseCollectionFile(File f);
    
    @GET
    @Path("/collection-match/{collectionId}")
    Response matchCollection(@PathParam("collectionId") int collectionId );
}
