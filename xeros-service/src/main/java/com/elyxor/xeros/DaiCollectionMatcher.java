package com.elyxor.xeros;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elyxor.xeros.model.CollectionClassificationMap;
import com.elyxor.xeros.model.CollectionClassificationMapDetail;
import com.elyxor.xeros.model.DaiMeterActual;
import com.elyxor.xeros.model.DaiMeterCollection;
import com.elyxor.xeros.model.DaiMeterCollectionDetail;
import com.elyxor.xeros.model.Machine;
import com.elyxor.xeros.model.repository.ActiveDaiRepository;
import com.elyxor.xeros.model.repository.ClassificationRepository;
import com.elyxor.xeros.model.repository.CollectionClassificationMapDetailRepository;
import com.elyxor.xeros.model.repository.CollectionClassificationMapRepository;
import com.elyxor.xeros.model.repository.DaiMeterActualRepository;
import com.elyxor.xeros.model.repository.DaiMeterCollectionDetailRepository;
import com.elyxor.xeros.model.repository.DaiMeterCollectionRepository;
import com.elyxor.xeros.model.repository.MachineRepository;

@Transactional
@Service
public class DaiCollectionMatcher {

	private static Logger logger = LoggerFactory.getLogger(DaiCollectionMatcher.class);

	@Autowired ActiveDaiRepository activeDaiRepository;
	@Autowired ClassificationRepository classificationRepository;
	@Autowired DaiMeterActualRepository daiMeterActualRepository;
	@Autowired DaiMeterCollectionRepository daiMeterCollectionRepo;
	@Autowired DaiMeterCollectionDetailRepository daiMeterCollectionDetailRepo;
	@Autowired CollectionClassificationMapRepository collectionClassificationMapRepo;
	@Autowired CollectionClassificationMapDetailRepository collectionClassificationMapDetailRepo;
	@Autowired MachineRepository machineRepository;
	

	
	public CollectionClassificationMap match(int collectionId) throws Exception {
		return this.match(daiMeterCollectionRepo.findOne(collectionId));
	}
	
	public CollectionClassificationMap match(DaiMeterCollection collectionData) throws Exception {
		if ( collectionData.getMachine() == null ) {
			List<Machine> machines = machineRepository.findByDaiDaiIdentifierAndMachineIdentifier(collectionData.getDaiIdentifier(), collectionData.getMachineIdentifier());			
			if ( machines != null && machines.size()>0 ) {
				Machine m = machines.iterator().next(); 
				if (collectionData.getLocationIdentifier().equals( String.valueOf(m.getLocation().getId()))) {
					collectionData.setMachine(m);
				} else {
					logger.warn("Mismatched locationID:{} for dai:{} machine:{}", collectionData.getLocationIdentifier(), collectionData.getDaiIdentifier(), collectionData.getMachineIdentifier());
				}
			}
			daiMeterCollectionRepo.save(collectionData);
		}
		if ( collectionData.getMachine() == null ) {
			throw new Exception(String.format("Unable to find the machine for collection %1s", collectionData.toString()));
		}
		Iterable<CollectionClassificationMap> existingCollections = this.collectionClassificationMapRepo.findByMachine(collectionData.getMachine());
		CollectionClassificationMap matchedMap = findMatches(collectionData, existingCollections);
		if ( matchedMap!=null ) {
			collectionData.setCollectionClassificationMap(matchedMap);
			daiMeterCollectionRepo.save(collectionData);
		}
		if ( collectionData.getCollectionClassificationMap()!=null && collectionData.getDaiMeterActual()==null) {
			collectionData.setDaiMeterActual(createDaiMeterActual(collectionData));
			daiMeterCollectionRepo.save(collectionData);
		}
		//if no matches found, map to 9999 and create dai actual record
		try {
			if (matchedMap==null && collectionData.getDaiMeterActual()==null) {
				Integer uc = collectionData.getMachine().getUnknownClass();
				if ( uc!=null ) {
					CollectionClassificationMap ccm = collectionClassificationMapRepo.findOne(uc);
					if (ccm!=null) {
						collectionData.setCollectionClassificationMap(ccm);
						collectionData.setDaiMeterActual(createDaiMeterActual(collectionData));
						daiMeterCollectionRepo.save(collectionData);
					}
				}
			}
		} catch (Exception ex) {
			logger.warn("Failed to find unknown map", ex);
		}
		return matchedMap;
	}
	
	
	private Float calculateRunTime(DaiMeterCollection c) {
		float startTime = c.getEarliestValue();
		DateTime endDt = new DateTime(c.getDaiCollectionTime());
		Duration duration = new Duration(endDt.withTimeAtStartOfDay(), endDt);
		
		float endTime = duration.toStandardSeconds().getSeconds();

		float runTime = endTime - startTime;		
		runTime = runTime>0?runTime:runTime + 86400;

		Machine m = c.getMachine();
		int startOffset = m.getStartTimeOffset()!=null?m.getStartTimeOffset():0;
		int endOffset = m.getStopTimeOffset()!=null?m.getStopTimeOffset():0;
		
		return runTime + startOffset + endOffset;

		
//		Machine m = c.getMachine();
//		Float runTime = new Float(0);
//		if ( m.getDoorLockMeterType() !=null ) {
//			for ( DaiMeterCollectionDetail cd : c.getCollectionDetails() ) {
//				if ( cd.getMeterType().equals(m.getDoorLockMeterType()) ) {
//					int startOffset = m.getStartTimeOffset()!=null?m.getStartTimeOffset():0;
//					int endOffset = m.getStopTimeOffset()!=null?m.getStopTimeOffset():0;
//					runTime += new Float(cd.getDuration() + startOffset + endOffset);
//				}
//			}
//			return runTime;
//		}
	}
	
	

	private Float calculateColdWater(DaiMeterCollection c) {
		Machine m = c.getMachine();
		if ( m.getDoorLockMeterType() !=null ) {
			for ( DaiMeterCollectionDetail cd : c.getCollectionDetails() ) {
				if ( cd.getMeterType().equals(m.getColdWaterMeterType()) ) {
					int waterOnly = m.getWaterOnly()!=null?m.getWaterOnly():0;
					return waterOnly==1?new Float(cd.getMeterValue()):new Float(cd.getDuration());
				}
			}
		}
		return new Float(0);
	}
	

	private Float calculateHotWater(DaiMeterCollection c) {
		Machine m = c.getMachine();
		if ( m.getDoorLockMeterType() !=null ) {
			for ( DaiMeterCollectionDetail cd : c.getCollectionDetails() ) {
				if ( cd.getMeterType().equals(m.getHotWaterMeterType()) ) {
					int waterOnly = m.getWaterOnly()!=null?m.getWaterOnly():0;
					return waterOnly==1?new Float(cd.getMeterValue()):new Float(cd.getDuration());
				}
			}
		}
		return new Float(0);
	}

	
	public DaiMeterActual createDaiMeterActual(DaiMeterCollection collectionData) throws Exception {
		// TODO : tons of checking for valid matches 
		DaiMeterActual daia = null;
		List<Machine> machines = this.machineRepository.findByDaiDaiIdentifierAndMachineIdentifier(collectionData.getDaiIdentifier(), collectionData.getMachineIdentifier());
		if ( machines!=null && machines.iterator().hasNext()) {
			daia = new DaiMeterActual();
			Machine m = machines.iterator().next();
			daia.setActiveDai(m.getDai());
			daia.setClassification(collectionData.getCollectionClassificationMap().getClassification());
			daia.setMachine(collectionData.getMachine());
			daia.setRunTime(new Float(calculateRunTime(collectionData)).intValue());
			daia.setColdWater(new Float(calculateColdWater(collectionData)).intValue());
			daia.setHotWater(new Float(calculateHotWater(collectionData)).intValue());
			daia.setTimestamp(collectionData.getDaiCollectionTime());
			daiMeterActualRepository.save(daia);
		} else {
			throw new Exception( String.format("no active dai found for [dai:%1s, machine: %2s]", collectionData.getDaiIdentifier(), collectionData.getMachine() ));
		}
		return daia;
	}
	
		
	
	private CollectionClassificationMap findMatches(DaiMeterCollection collectionData, Iterable<CollectionClassificationMap> existingCollections) {
		CollectionClassificationMap matchedMap = null;
		try {
			if ( existingCollections!=null && existingCollections.iterator().hasNext() ) {
				Machine collectionMachine = collectionData.getMachine();
				List<CollectionClassificationMapDetail> normalizedDetails = normalizeCollectionDetails(collectionData, collectionMachine);
				// for each collection...
				for ( CollectionClassificationMap collMap : existingCollections ) {
					//if size of collection and map do not match, stop
					if (collMap.getCollectionDetails().size() != normalizedDetails.size()) {
						continue;
					}
					// validate all values...
					int matches = 0;
					for(CollectionClassificationMapDetail collMapDetail : collMap.getCollectionDetails() ) {
						for ( CollectionClassificationMapDetail normalizedDetail : normalizedDetails ) {
							logger.info(String.format("MATCH?  E: %1s == NEW: %2s", collMapDetail.toString(), normalizedDetail.toString()) );							
							int startVariance = (collectionMachine.getSensorStartTimeVariance()!=null?collectionMachine.getSensorStartTimeVariance():0);
							int durationVariance = collectionMachine.getDoorLockMeterType().equals(collMapDetail.getMeterType())?
									(collectionMachine.getDoorLockDurationMatchVariance()!=null?collectionMachine.getDoorLockDurationMatchVariance():60):collectionMachine.getDurationMatchVariance();						
							int useStartTime = collectionMachine.getUseStartTime()!=null?collectionMachine.getUseStartTime():0;
							if ( normalizedDetail.matches(collMapDetail, startVariance, durationVariance, useStartTime) ) {
								logger.info(String.format("MATCHED!"));
								matches++;
								break;
							}
						}
					}
					if (matches == normalizedDetails.size()) {
						matchedMap = collMap;
						break;
					} else {
						logger.info(String.format("no match..."));
					}
				}
			}
		} catch (Exception e) {
			logger.warn("Failed to match", e);
		}
		return matchedMap;
	}
	
	private List<CollectionClassificationMapDetail> normalizeCollectionDetails(DaiMeterCollection collection, Machine machine) {
		Collection<DaiMeterCollectionDetail> collDetails = collection.getCollectionDetails();
		List<CollectionClassificationMapDetail> normalizedDetails = new ArrayList<CollectionClassificationMapDetail>();		
		float earliestValue = Float.MAX_VALUE;
		
		//get ignore meters and split into array
		String ignoreMeterType = machine.getIgnoreMeterType();
		String[] ignoreMeterTypes = {"string"};
		
		if (ignoreMeterType!=null) {
			ignoreMeterTypes = ignoreMeterType.split(",");
		}
		
		for (DaiMeterCollectionDetail collectionDetail : collDetails) {
			if ( collectionDetail.getMeterType().startsWith("WM")) {
				continue;
			}
			
			if ( collectionDetail.getDuration()==0) {
				continue;
			}
			earliestValue = ( collectionDetail.getMeterValue()<earliestValue )?collectionDetail.getMeterValue():earliestValue;
		}
		for (DaiMeterCollectionDetail collectionDetail : collDetails) {
			if (collectionDetail.getMeterType().startsWith("WM") ||
					collectionDetail.getDuration()==0 ||
					//check ignore array
					Arrays.asList(ignoreMeterTypes).contains(collectionDetail.getMeterType()) ||
					collectionDetail.getMeterType().equals(machine.getDoorLockMeterType())) {
				continue;
			}
			float normalizedValue = (collectionDetail.getMeterValue() == earliestValue || collectionDetail.getMeterType().startsWith("WM") )?0:collectionDetail.getMeterValue()-earliestValue;
			CollectionClassificationMapDetail ccd = new CollectionClassificationMapDetail();
			ccd.setMeterType(collectionDetail.getMeterType());
			ccd.setStartTime(normalizedValue);
			ccd.setDuration(collectionDetail.getDuration());
			normalizedDetails.add(ccd);
			
		}
		collection.setEarliestValue(earliestValue);

		return normalizedDetails;		
	}

	
	public CollectionClassificationMap createCollectionClassificationMap(int collectionId, int classificationId) throws Exception {
		DaiMeterCollection dmc = this.daiMeterCollectionRepo.findOne(collectionId);
		if ( dmc.getMachine()==null ) {
			throw new Exception(String.format("The machine for collectionId %1s is unknown.  Mapping not created.", collectionId));
		}
		CollectionClassificationMap ccm = new CollectionClassificationMap();
		ccm.setMachine(dmc.getMachine());
		ccm.setClassification(classificationRepository.findOne(classificationId));
		ccm.setCollectionDetails(normalizeCollectionDetails(dmc, ccm.getMachine()));
		for ( CollectionClassificationMapDetail ccmd : ccm.getCollectionDetails() ) {
			ccmd.setCollectionClassificationMap(ccm);
		}
		this.collectionClassificationMapRepo.save(ccm);
		return ccm;
	}

	public boolean unmatch(int collectionId) {
		DaiMeterCollection dmc = daiMeterCollectionRepo.findOne(collectionId);
		daiMeterActualRepository.delete(dmc.getDaiMeterActual());
		dmc.setCollectionClassificationMap(null);
		dmc.setDaiMeterActual(null);
		daiMeterCollectionRepo.save(dmc);
		return true;
	}

	public List<CollectionClassificationMapDetail> normalize(int collectionId) {
		DaiMeterCollection dmc = this.daiMeterCollectionRepo.findOne(collectionId);
		return this.normalizeCollectionDetails(dmc, dmc.getMachine());
	}

}
