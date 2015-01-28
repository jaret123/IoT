package com.elyxor.xeros;

import com.elyxor.xeros.model.CollectionClassificationMap;
import com.elyxor.xeros.model.DaiMeterCollection;
import com.elyxor.xeros.model.Machine;
import com.elyxor.xeros.model.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.util.List;
import java.util.Map;

@Service("rssvc")
public class RSServiceImpl implements RSService {

	private static Logger logger = LoggerFactory.getLogger(RSService.class);
	@Autowired DaiCollectionParser daiCollectionParser;
	@Autowired DaiCollectionMatcher daiCollectionMatcher;
	@Autowired DaiStatus daiStatus;

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
	public Response ping(String daiIdentifier) {
		ResponseBuilder r = Response.ok();
		try {
			boolean success = daiStatus.receivePing(daiIdentifier);
			r.entity(success);
		} catch (Exception e) {
			r = Response.serverError().entity(e.toString());
		}
		return r.build();
	}

	@Override
	public Response pingStatus() {
		ResponseBuilder r = Response.ok();
		try {
			r.entity(daiStatus.pingStatus());
		} catch (Exception e) {
			r = Response.serverError().entity(e.toString());
		}
		return r.build();
	}

    @Override
    public Response receiveMachineStatus(String daiIdentifier, byte xerosStatus, byte stdStatus) {
        ResponseBuilder r = Response.ok();
        try {
            r.entity(daiStatus.receiveMachineStatus(daiIdentifier, xerosStatus, stdStatus));
        } catch (Exception e) {
            r = Response.serverError().entity(e.toString());
        }
        return r.build();
    }

    @Override
    public Response getStatus(List<Integer> machineIdList) {
        ResponseBuilder r = Response.ok();
        try {
            List<Status> statusList = daiStatus.getStatus(machineIdList);
            r.entity(statusList);
        } catch (Exception e) {
            r = Response.serverError().entity(e.toString());
        }
        return r.build();
    }

    @Override
    public Response getStatusHistory(List<Integer> machineIdList) {
        ResponseBuilder r = Response.ok();
        try {
            List<Status> statusList = daiStatus.getStatusHistory(machineIdList);
            r.entity(statusList);
        } catch (Exception e) {
            r = Response.serverError().entity(e.toString());
        }
        return r.build();
    }

    @Override
    public Response getStatusGaps(List<Machine> machineList) {
        ResponseBuilder r = Response.ok();
        try {
            List<Status> statusList = daiStatus.getStatusGaps(machineList);
            r.entity(statusList);
        } catch (Exception e) {
            r = Response.serverError().entity(e.toString());
        }
        return r.build();
    }
    @Override
    public Response getStatusGaps() {
        ResponseBuilder r = Response.ok();
        try {
            r.entity(daiStatus.getStatusGaps()).header("Content-Disposition", "attachment; filename=statusgaps.xls");
        } catch (Exception e) {
            r = Response.serverError().entity(e.toString());
        }
        return r.build();
    }

    @Override
    public Response getSimpleCycleReport(UriInfo info) {
        ResponseBuilder r = Response.ok();
        String machine = info.getQueryParameters().getFirst("machine");
        String company = info.getQueryParameters().getFirst("company");
        String location = info.getQueryParameters().getFirst("location");
        if (machine != null && company != null) {
            return Response.ok("Cannot use machine and company together", MediaType.TEXT_PLAIN).build();
        }
        else if (machine != null && location != null) {
            return Response.ok("Cannot use machine and location together", MediaType.TEXT_PLAIN).build();
        }
        else if (company != null && location != null) {
            return Response.ok("Cannot use company and location together", MediaType.TEXT_PLAIN).build();
        }
        else {
            try {
                File result = daiStatus.getCycleReports(info);
                if (result == null) {
                    return Response.ok("No records found for this query.", MediaType.TEXT_PLAIN).build();
                }
                else r = r.entity(result).header("Content-Disposition", "attachment; filename=cycleReport.xls");
            } catch (Exception e) {
                r = Response.serverError().entity(e.toString());
            }
            return r.build();
        }
    }

    @Override
    public Response getLastLog() {
        ResponseBuilder r = Response.ok();
        try {
            r.entity(daiStatus.getLastLog()).header("Content-Disposition", "attachment; filename=lastLogReport.xls");
        } catch (Exception e) {
            r = Response.serverError().entity(e.toString());
        }
        return r.build();
    }
}
