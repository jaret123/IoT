package com.elyxor.xeros;

import java.io.File;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Service;

@Path("/rs")
@Consumes({"application/json"})
@Produces({"application/json"})
@Service
public interface RSService {

    @GET
    @Path("/healthcheck")
    Response healthcheck();    
    
    Response parseCollectionFile(File f);
}
