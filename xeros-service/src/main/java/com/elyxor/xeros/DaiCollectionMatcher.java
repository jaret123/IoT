package com.elyxor.xeros;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elyxor.xeros.model.ActiveDai;
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
			List<Machine> machines = machineRepository.findByLocationIdAndMachineIdentifier(Integer.parseInt(collectionData.getLocationIdentifier()), 
					collectionData.getMachineIdentifier());
			if ( machines != null && machines.size()>0 ) {
				collectionData.setMachine(machines.iterator().next());
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
		return matchedMap;
	}
	
	
	private Float calculateRunTime(DaiMeterCollection c) {
		Machine m = c.getMachine();
		if ( m.getDoorLockMeterType() !=null ) {
			for ( DaiMeterCollectionDetail cd : c.getCollectionDetails() ) {
				if ( cd.getMeterType().equals(m.getDoorLockMeterType()) ) {
					int startOffset = m.getStartTimeOffset()!=null?m.getStartTimeOffset():0;
					int endOffset = m.getStopTimeOffset()!=null?m.getStopTimeOffset():0;
					return new Float(cd.getDuration() + startOffset + endOffset);
				}
			}
		}
		return new Float(0);
	}
	
	

	private Float calculateColdWater(DaiMeterCollection c) {
		Machine m = c.getMachine();
		if ( m.getDoorLockMeterType() !=null ) {
			for ( DaiMeterCollectionDetail cd : c.getCollectionDetails() ) {
				if ( cd.getMeterType().equals(m.getColdWaterMeterType()) ) {
					return new Float(cd.getDuration());
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
					return new Float(cd.getDuration());
				}
			}
		}
		return new Float(0);
	}

	
	public DaiMeterActual createDaiMeterActual(DaiMeterCollection collectionData) throws Exception {
		// TODO : tons of checking for valid matches 
		DaiMeterActual daia = null;
		List<ActiveDai> dais = this.activeDaiRepository.findByDaiIdentifierAndMachine(collectionData.getDaiIdentifier(), collectionData.getMachine());
		if ( dais!=null && dais.iterator().hasNext()) {
			daia = new DaiMeterActual();
			daia.setActiveDai(dais.iterator().next());
			daia.setClassification(collectionData.getCollectionClassificationMap().getClassification());
			daia.setMachine(collectionData.getMachine());
			daia.setRunTime(new Float(calculateRunTime(collectionData)).intValue());
			daia.setColdWater(new Float(calculateColdWater(collectionData)).intValue());
			daia.setHotWater(new Float(calculateHotWater(collectionData)).intValue());
			daiMeterActualRepository.save(daia);
		} else {
			throw new Exception( String.format("no active dai found for [dai:%1s, machine: %2s]", collectionData.getDaiIdentifier(), collectionData.getMachine() ));
		}
		return daia;
	}
	
		
	
	private CollectionClassificationMap findMatches(DaiMeterCollection collectionData, Iterable<CollectionClassificationMap> existingCollections) {
		CollectionClassificationMap matchedMap = null;
		if ( existingCollections!=null && existingCollections.iterator().hasNext() ) {
			List<CollectionClassificationMapDetail> normalizedDetails = normalizeCollectionDetails(collectionData.getCollectionDetails());
			// for each collection...
			for ( CollectionClassificationMap collMap : existingCollections ) {
				// validate all values...
				int matches = 0;
				for(CollectionClassificationMapDetail collMapDetail : collMap.getCollectionDetails() ) {
					for ( CollectionClassificationMapDetail normalizedDetail : normalizedDetails ) {
						logger.info(String.format("MATCH?  E: %1s == NEW: %2s", collMapDetail.toString(), normalizedDetail.toString()) );
						if ( normalizedDetail.matches(collMapDetail) ) {
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
		return matchedMap;
	}
	
	private List<CollectionClassificationMapDetail> normalizeCollectionDetails(Collection<DaiMeterCollectionDetail> collDetails) {
		List<CollectionClassificationMapDetail> normalizedDetails = new ArrayList<CollectionClassificationMapDetail>();		
		float earliestValue = Float.MAX_VALUE;
		for (DaiMeterCollectionDetail collectionDetail : collDetails) {
			if ( collectionDetail.getMeterType().startsWith("WM")) {
				continue;
			}
			earliestValue = ( collectionDetail.getMeterValue()<earliestValue )?collectionDetail.getMeterValue():earliestValue;
		}
		for (DaiMeterCollectionDetail collectionDetail : collDetails) {
			if (collectionDetail.getMeterType().startsWith("WM") || collectionDetail.getMeterType().equals("SENSOR_4")) {
				continue;
			}
			float normalizedValue = (collectionDetail.getMeterValue() == earliestValue || collectionDetail.getMeterType().startsWith("WM") )?0:collectionDetail.getMeterValue()-earliestValue;
			CollectionClassificationMapDetail ccd = new CollectionClassificationMapDetail();
			ccd.setMeterType(collectionDetail.getMeterType());
			ccd.setStartTime(normalizedValue);
			ccd.setDuration(collectionDetail.getDuration());
			normalizedDetails.add(ccd);
			
		}
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
		ccm.setCollectionDetails(normalizeCollectionDetails(dmc.getCollectionDetails()));
		for ( CollectionClassificationMapDetail ccmd : ccm.getCollectionDetails() ) {
			ccmd.setCollectionClassificationMap(ccm);
		}
		this.collectionClassificationMapRepo.save(ccm);
		return ccm;
	}

}
