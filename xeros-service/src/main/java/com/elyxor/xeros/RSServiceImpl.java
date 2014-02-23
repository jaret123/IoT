package com.elyxor.xeros;

import java.io.File;
import java.util.Map;

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
	public Response parseCollectionFile(File f, Map<String, String> fileMeta) {
		try {
			daiCollectionParser.parse(f, fileMeta);
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
