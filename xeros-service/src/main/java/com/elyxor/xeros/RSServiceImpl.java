package com.elyxor.xeros;

import java.io.File;

import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RSServiceImpl implements RSService {
	
	@Autowired DaiCollectionParser daiCollectionParser;

	@Override
	public Response healthcheck() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response parseCollectionFile(File f) {
		try {
			daiCollectionParser.parse(f);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Response.ok().build();
	}

}
