package com.elyxor.xeros;

import java.io.File;
import java.io.FileReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

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
	
	private static Logger logger = LoggerFactory.getLogger(DaiCollectionParser.class);
	@Autowired DaiMeterCollectionRepository daiMeterCollectionRepo;
	@Autowired DaiMeterCollectionDetailRepository daiMeterCollectionDetailRepo;
	@Autowired DaiCollectionMatcher matcher;
	
	class CollectionData {
		String[] fileHeader = null;
		float fileWriteTime;
		String[] elementHeaders;
		List<Integer> sensorEventCounts;
		List<List<Float>> sensorEventData = new ArrayList<List<Float>>();
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
		String[] lines = fString.toString().split("\r");
		List<String> collectionLines = new ArrayList<String>();
		for(String line: lines) {
			dmc = new DaiMeterCollection();
			parsedCollections.add(dmc);
			dmc.setLocationIdentifier(fileMeta.get("location_id"));
			dmc.setOlsonTimezoneId(fileMeta.get("olson_timezone_id"));
			dmc.setFileUploadTime(new Timestamp( Long.parseLong(fileMeta.get("current_system_time")) ));
			dmc.setFileCreateTime(new Timestamp( Long.parseLong(fileMeta.get("file_create_time")) ));

			if ( line.trim().startsWith("File Write") && collectionLines.size()>1 ) {
				String origHeader = collectionLines.get(collectionLines.size()-1);
				createCollectionModels(dmc, collectionLines);
				collectionLines.clear();
				collectionLines.add(origHeader);
			}
			collectionLines.add(line);
		}
		dmc = createCollectionModels(dmc, collectionLines);
		return parsedCollections;
	}
	
	
	private DaiMeterCollection createCollectionModels(DaiMeterCollection dmc, List<String> lines) {
		long midnightInMilliseconds = getMidnightMillis();
		CollectionData cd = new CollectionData();		 
		boolean inEventData = false;
		for ( String line : lines ) {
			if (StringUtils.isBlank(line)) {
				continue;
			}
			String[] lineData = line.split(",");
			if ( cd.fileHeader == null ) {
				cd.fileHeader = lineData;
				dmc.setDaiIdentifier(cd.fileHeader[0]);
				dmc.setMachineIdentifier(cd.fileHeader[1]);
			}
			String firstEle = lineData[0].trim();
						
			if ( inEventData ) {
				if (firstEle.contains("Event Count")) {
					inEventData = false;
					cd.sensorEventCounts = new ArrayList<Integer>();
					for( String eCount : lineData) {
						try {
							cd.sensorEventCounts.add(Integer.parseInt(eCount));
						} catch(NumberFormatException nfe) {}
					}
				} else {
					List<Float> eventData = new ArrayList<Float>();
					for( String eValue : lineData) {
						try {
							eventData.add(Float.parseFloat(eValue));
						} catch(NumberFormatException nfe) {}
					}
					cd.sensorEventData.add(eventData);
				}
			}			
			else if ( firstEle.startsWith("File Write") && lineData.length>1 ) {
				cd.fileWriteTime = Float.parseFloat(lineData[1].trim());
				dmc.setDaiCollectionTime( new Timestamp( midnightInMilliseconds+ ((int)cd.fileWriteTime*1000) ) );
				logger.info(String.format("storing collection data for run %1s", dmc ));
			}
			else if ( firstEle.equals("Event") ) {
				cd.elementHeaders = new String[lineData.length];
				for (int lcv = 0; lcv < lineData.length; lcv++ ) {
					cd.elementHeaders[lcv] = lineData[lcv].trim();
				}
				inEventData = true;
			}
			else if ( firstEle.startsWith("WM") ) {				
				cd.wmData.add(lineData);
			}
		}
		
		List<DaiMeterCollectionDetail> collectionData = new ArrayList<DaiMeterCollectionDetail>(); 
		for( int sensorIx = 0; sensorIx < cd.sensorEventData.size(); sensorIx++ ) {			
			for( int lcv=0; lcv < cd.sensorEventCounts.size(); lcv++ ) {
				float start = cd.sensorEventData.get(sensorIx).get(lcv*2+1);
				float duration = cd.sensorEventData.get(sensorIx).get(lcv*2+2);
				if ( start > 0 ) {
					DaiMeterCollectionDetail dmcd = new DaiMeterCollectionDetail();
					dmcd.setMeterType(String.format("SENSOR_%1s", lcv+1));
					dmcd.setMeterValue(start);					
					dmcd.setDuration(duration);
					dmcd.setTimestamp(new Timestamp(midnightInMilliseconds + new Float(start).longValue()*1000));
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
		} catch (Exception e) {
			logger.info("no matched collection map found");
		}
		daiMeterCollectionRepo.save(dmc);
		return dmc;
	}
	
}
