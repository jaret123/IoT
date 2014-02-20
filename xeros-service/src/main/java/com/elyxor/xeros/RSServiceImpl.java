package com.elyxor.xeros;

import java.io.File;

import javax.jws.WebService;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("rssvc")
public class RSServiceImpl implements RSService {
	
	@Autowired DaiCollectionParser daiCollectionParser;
	@Autowired DaiCollectionMatcher daiCollectionMatcher;

	@Override
	public Response healthcheck() {
		return Response.ok().build();
	}

	@Override
	public Response parseCollectionFile(File f) {
		try {
			daiCollectionParser.parse(f);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Response.ok().build();
	}

	@Override
	public Response matchCollection(int collectionId) {
		int matchedId = daiCollectionMatcher.match(collectionId);
		return Response.ok().entity(new Integer(matchedId)).build();
	}

}
