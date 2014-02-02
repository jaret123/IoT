package com.elyxor.xeros;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/rs")
@Consumes({"application/json"})
@Produces({"application/json"})
public interface RSService {

    @GET
    @Path("/healthcheck")
    Response healthcheck();    
}
