package com.elyxor.xeros;

import java.io.File;
import java.io.FileReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elyxor.xeros.model.DaiMeterCollection;
import com.elyxor.xeros.model.DaiMeterCollectionDetail;
import com.elyxor.xeros.model.DaiMeterCollectionDetailRepository;
import com.elyxor.xeros.model.DaiMeterCollectionRepository;


@Transactional
@Service
public class DaiCollectionParser {
	
	private static Logger logger = LoggerFactory.getLogger(DaiCollectionParser.class);
	@Autowired DaiMeterCollectionRepository daiMeterCollectionRepo;
	@Autowired DaiMeterCollectionDetailRepository daiMeterCollectionDetailRepo;
	
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
	
	public void parse(File file) throws Exception {
		byte[] inputData = IOUtils.toByteArray(new FileReader(file));		
		StringBuffer fString = new StringBuffer();
		for ( byte b : inputData ){
			if( (int)b<10 ) {
				continue;
			}
			fString.append((char)b);
		}		
		String[] lines = fString.toString().split("\r");
		List<String> collectionLines = new ArrayList<String>();
		for(String line: lines) {
			if ( line.trim().startsWith("File Write") && collectionLines.size()>1 ) {
				String origHeader = collectionLines.get(collectionLines.size()-1);
				createCollectionModels(collectionLines);
				collectionLines.clear();
				collectionLines.add(origHeader);
			}
			collectionLines.add(line);
		}
		createCollectionModels(collectionLines);
	}
	
	
	private void createCollectionModels(List<String> lines) {
		long midnightInMilliseconds = getMidnightMillis();
		CollectionData cd = new CollectionData();
		DaiMeterCollection dmc = new DaiMeterCollection();
		boolean inEventData = false;
		for ( String line : lines ) {
			if (StringUtils.isBlank(line)) {
				continue;
			}
			String[] lineData = line.split(",");
			if ( cd.fileHeader == null ) {
				cd.fileHeader = lineData;
				dmc.setMachineName(cd.fileHeader[0]);
				dmc.setMachineType(cd.fileHeader[1]);
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
				dmc.setCollectionTime( new Timestamp( midnightInMilliseconds+ ((int)cd.fileWriteTime*1000) ) );
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
			dmcd.setTimestamp(dmc.getCollectionTime());
			collectionData.add(dmcd);
		}
		
		daiMeterCollectionRepo.save(dmc);
		for(DaiMeterCollectionDetail dmcd : collectionData) {
			dmcd.setDaiMeterCollection(dmc);
			daiMeterCollectionDetailRepo.save(dmcd);			
		}
	}
	
}
