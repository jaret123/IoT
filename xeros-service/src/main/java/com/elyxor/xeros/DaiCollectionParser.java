package com.elyxor.xeros;

import java.io.File;
import java.io.FileReader;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elyxor.xeros.model.CollectionClassificationMap;
import com.elyxor.xeros.model.DaiMeterCollection;
import com.elyxor.xeros.model.DaiMeterCollectionDetail;
import com.elyxor.xeros.model.repository.DaiMeterCollectionDetailRepository;
import com.elyxor.xeros.model.repository.DaiMeterCollectionRepository;


@Transactional
@Service
public class DaiCollectionParser {
	
	SimpleDateFormat daiSdf = new SimpleDateFormat("HH : mm : ss");
	
	private static Logger logger = LoggerFactory.getLogger(DaiCollectionParser.class);
	@Autowired DaiMeterCollectionRepository daiMeterCollectionRepo;
	@Autowired DaiMeterCollectionDetailRepository daiMeterCollectionDetailRepo;
	@Autowired DaiCollectionMatcher matcher;
	
	class CollectionData {
		String[] fileHeader = null;
		float fileWriteTime;
		String[] elementHeaders;
		List<Integer> sensorEventCounts;
		List<List<String>> sensorEventData = new ArrayList<List<String>>();
		String[] totals;
		List<String[]> wmData = new ArrayList<String[]>();
	}
	
	
	private long getMidnightMillis() { 
		Calendar today = new GregorianCalendar();
		today.setTime(new Date());
		today.set(Calendar.HOUR_OF_DAY, 0);
		today.set(Calendar.MINUTE, 0);
		today.set(Calendar.SECOND, 0);
		today.set(Calendar.MILLISECOND, 0);
		return today.getTimeInMillis();
	}
	
	public List<DaiMeterCollection> parse(File file, Map<String, String> fileMeta) throws Exception {
		byte[] inputData = IOUtils.toByteArray(new FileReader(file));		
		StringBuffer fString = new StringBuffer();
		for ( byte b : inputData ){
			if( (int)b<10 ) {
				continue;
			}
			fString.append((char)b);
		}
	    
		List<DaiMeterCollection> parsedCollections = new ArrayList<DaiMeterCollection>();
		DaiMeterCollection dmc = null;
		String fLines = fString.toString().replaceAll("\r", "\n");
		String[] lines = fLines.split("\n");
		List<String> collectionLines = new ArrayList<String>();
		for(String line: lines) {
			if ( dmc==null ) {
				dmc = new DaiMeterCollection();
				parsedCollections.add(dmc);
				dmc.setLocationIdentifier(fileMeta.get("location_id"));
				dmc.setOlsonTimezoneId(fileMeta.get("olson_timezone_id"));
				dmc.setFileUploadTime(new Timestamp( Long.parseLong(fileMeta.get("current_system_time")) ));
				dmc.setFileCreateTime(new Timestamp( Long.parseLong(fileMeta.get("file_create_time")) ));
			}

			if ( line.trim().startsWith("File Write") && collectionLines.size()>1 ) {
				String origHeader = collectionLines.get(collectionLines.size()-1);
				createCollectionModels(dmc, collectionLines);
				collectionLines.clear();
				dmc = null;
				collectionLines.add(origHeader);
			}
			collectionLines.add(line);
		}
		try {
			dmc = createCollectionModels(dmc, collectionLines);
		} catch (Exception ex) {
			logger.warn("Failed to save", ex);
		}
		return parsedCollections;
	}
	
	
	private DaiMeterCollection createCollectionModels(DaiMeterCollection dmc, List<String> lines) {
		long midnightInMilliseconds = getMidnightMillis();
		CollectionData cd = new CollectionData();		 
		boolean inEventData = false;
		for ( String line : lines ) {
			if (StringUtils.isBlank(line)) {
				if ( inEventData ) {
					inEventData = false;
				}
				continue;
			}
			String[] lineData = line.split(",");
			String firstEle = lineData[0].trim();
			
			if ( cd.fileHeader == null ) {
				cd.fileHeader = lineData;
				dmc.setDaiIdentifier(StringUtils.trim(cd.fileHeader[0]));
				dmc.setMachineIdentifier(StringUtils.trim(cd.fileHeader[1]));
			} else if ( firstEle.startsWith("File Write") && lineData.length>1 ) {
				try {
					Calendar c = parseTimestamp(lineData[1].trim(), dmc.getOlsonTimezoneId());
					cd.fileWriteTime = c.getTimeInMillis();
					dmc.setDaiCollectionTime( new Timestamp( c.getTimeInMillis() ) );
				} catch (ParseException ex) {
					cd.fileWriteTime = Float.parseFloat(lineData[1].trim());
					dmc.setDaiCollectionTime( new Timestamp( midnightInMilliseconds+ ((int)cd.fileWriteTime*1000) ) );
				}
				logger.info(String.format("storing collection data for run %1s", dmc ));
				inEventData = true;
			}
			else if ( firstEle.equals("Event") ) {
				cd.elementHeaders = new String[lineData.length];
				for (int lcv = 0; lcv < lineData.length; lcv++ ) {
					cd.elementHeaders[lcv] = lineData[lcv].trim();
				}				
			}
			else if ( firstEle.startsWith("WM") ) {				
				cd.wmData.add(lineData);
			}
			else if (inEventData) {
				List<String> eventData = new ArrayList<String>();
				for( String eValue : lineData) {
					try {
						eventData.add(eValue);
					} catch(NumberFormatException nfe) {}
				}
				cd.sensorEventData.add(eventData);
			} else {
				List eCounts = new ArrayList<Integer>();				 
				for( String eCount : lineData) {
					try {
						eCounts.add(Integer.parseInt(StringUtils.trim(eCount)));
					} catch(NumberFormatException nfe) {
						logger.debug(eCount, nfe);
					}
				}
				cd.sensorEventCounts = eCounts;
			}
		}
		
		List<DaiMeterCollectionDetail> collectionData = new ArrayList<DaiMeterCollectionDetail>(); 
		for( int sensorIx = 0; sensorIx < cd.sensorEventData.size(); sensorIx++ ) {			
			for( int lcv=0; lcv < cd.sensorEventCounts.size(); lcv++ ) {
				String startStr = StringUtils.trim(cd.sensorEventData.get(sensorIx).get(lcv*2+2));
				Calendar startTs = null;
				float start = 0;
				try {
					try {
						startTs = parseTimestamp(startStr, dmc.getOlsonTimezoneId());
						start = startTs.getTimeInMillis()-midnightInMilliseconds;
						start = start>0?start/1000:start;
					} catch (ParseException ex) {
						start = Float.parseFloat(startStr);
					}
				} catch (Exception ex) {
					logger.debug("Failed to parse {}", startStr);
				}
				String durStr = StringUtils.trim(cd.sensorEventData.get(sensorIx).get(lcv*2+3));
				float duration = 0;
				try {
					try {
						Calendar c = parseTimestamp(durStr, dmc.getOlsonTimezoneId());
						duration = c.getTimeInMillis()-midnightInMilliseconds;
						duration = duration>0?duration/1000:duration;
					} catch (ParseException ex) {
						duration = Float.parseFloat(startStr);
					}
				} catch (Exception ex) {
					logger.debug("Failed to parse {}", startStr);
				}				
				if ( start >= 0 ) {
					DaiMeterCollectionDetail dmcd = new DaiMeterCollectionDetail();
					dmcd.setMeterType(String.format("SENSOR_%1s", lcv+1));
					dmcd.setMeterValue(start);					
					dmcd.setDuration(duration);
					dmcd.setTimestamp(new Timestamp(startTs!=null?startTs.getTimeInMillis():midnightInMilliseconds));
					collectionData.add(dmcd);
				}
			}			
		}	
		for(String[] wmEntry : cd.wmData) {
			DaiMeterCollectionDetail dmcd = new DaiMeterCollectionDetail();
			dmcd.setMeterType(wmEntry[0].trim().replaceAll(" ", "").replaceAll(":", ""));
			dmcd.setMeterValue(Float.parseFloat(wmEntry[1]));
			dmcd.setDuration(Float.parseFloat(wmEntry[2]));
			dmcd.setTimestamp(dmc.getDaiCollectionTime());
			collectionData.add(dmcd);
		}
		
		daiMeterCollectionRepo.save(dmc);
		
		if ( collectionData.size()>0 ) {
			for(DaiMeterCollectionDetail dmcd : collectionData) {
				dmcd.setDaiMeterCollection(dmc);
				daiMeterCollectionDetailRepo.save(dmcd);			
			}
			dmc.setCollectionDetails(collectionData);			
			try {
				CollectionClassificationMap ccm = matcher.match(dmc);
				if ( ccm != null ) {
					dmc.setCollectionClassificationMap(ccm);
				}
				daiMeterCollectionRepo.save(dmc);
			} catch (Exception e) {
				logger.info("no matched collection map found");
			}			
		}
		return dmc;
	}
	
	private Calendar parseTimestamp(String ts, String olsonTz) throws ParseException {
		Calendar now = new GregorianCalendar();
		now.setTime(new Date());
		Date parsedDate = daiSdf.parse(ts.trim());
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone(olsonTz));
		c.setTime(parsedDate);
		c.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));				
		
		logger.info(new SimpleDateFormat("yyyy-MM-dd.HH:mm:ss").format(c.getTime()));
		return c;
	}
	
}
