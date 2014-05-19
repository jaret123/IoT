package com.elyxor.xeros;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.elyxor.xeros.model.CollectionClassificationMap;
import com.elyxor.xeros.model.DaiMeterCollection;

@Service("rssvc")
public class RSServiceImpl implements RSService {

	private static Logger logger = LoggerFactory.getLogger(RSService.class);
	@Autowired DaiCollectionParser daiCollectionParser;
	@Autowired DaiCollectionMatcher daiCollectionMatcher;

	@Override
	public Response healthcheck() {
		return Response.ok().build();
	}

	@Override
	public Response parseCollectionFile(File f, Map<String, String> fileMeta) {
		ResponseBuilder r = Response.ok();
		try {
			List<DaiMeterCollection> parsedCollections = daiCollectionParser.parse(f, fileMeta);
			r.entity(parsedCollections);
		} catch (Exception e) {
			logger.info("Failed to save", e);
			r = Response.serverError().entity(e.toString());
		}
		return r.build();
	}

	@Override
	public Response matchCollection(int collectionId) {
		ResponseBuilder r = Response.ok();
		try {
			CollectionClassificationMap ccm = daiCollectionMatcher.match(collectionId);
			r.entity(ccm);
		} catch (Exception e) {
			r = Response.serverError().entity(e.toString());
		}
		return r.build();
	}

	@Override
	public Response createCollectionClassificationMap(int collectionId, int classificationId) {
		ResponseBuilder r = Response.ok();
		try {
			CollectionClassificationMap ccm = daiCollectionMatcher.createCollectionClassificationMap(collectionId, classificationId);
			r.entity(ccm);
		} catch (Exception e) {
			r = Response.serverError().entity(e.toString());
		}
		return r.build();
	}

	@Override
	public Response unmatchCollection(int collectionId) {
		ResponseBuilder r = Response.ok();
		try {
			boolean success = daiCollectionMatcher.unmatch(collectionId);
			r.entity(success);
		} catch (Exception e) {
			r = Response.serverError().entity(e.toString());
		}
		return r.build();
	}

	@Override
	public Response normalizeCollection(int collectionId) {
		ResponseBuilder r = Response.ok();
		try {
			r.entity(daiCollectionMatcher.normalize(collectionId));
		} catch (Exception e) {
			r = Response.serverError().entity(e.toString());
		}
		return r.build();
	}

	@Override
	public Response ping(DaiStatus daiStatus) {
		// TODO Auto-generated method stub
		return null;
	}

}
