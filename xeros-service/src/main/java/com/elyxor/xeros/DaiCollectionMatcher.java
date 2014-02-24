package com.elyxor.xeros;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import com.elyxor.xeros.model.repository.ClassificationRepository;
import com.elyxor.xeros.model.repository.CollectionClassificationMapDetailRepository;
import com.elyxor.xeros.model.repository.CollectionClassificationMapRepository;
import com.elyxor.xeros.model.repository.DaiMeterCollectionDetailRepository;
import com.elyxor.xeros.model.repository.DaiMeterCollectionRepository;

@Transactional
@Service
public class DaiCollectionMatcher {

	private static Logger logger = LoggerFactory.getLogger(DaiCollectionMatcher.class);

	@Autowired ClassificationRepository classificationRepository;
	@Autowired DaiMeterCollectionRepository daiMeterCollectionRepo;
	@Autowired DaiMeterCollectionDetailRepository daiMeterCollectionDetailRepo;
	@Autowired CollectionClassificationMapRepository collectionClassificationMapRepo;
	@Autowired CollectionClassificationMapDetailRepository collectionClassificationMapDetailRepo;
	
	
	public CollectionClassificationMap match(int collectionId) throws Exception {
		return this.match(daiMeterCollectionRepo.findOne(collectionId));
	}
	
	public CollectionClassificationMap match(DaiMeterCollection collectionData) throws Exception {
		if ( collectionData.getMachine() == null ) {
			collectionData.setMachine(findMachine(collectionData.getDaiIdentifier(), collectionData.getMachineIdentifier()));
		}
		if ( collectionData.getMachine() == null ) {
			throw new Exception(String.format("Unable to find the machine for collection %1s", collectionData.toString()));
		}
		Iterable<CollectionClassificationMap> existingCollections = this.collectionClassificationMapRepo.findByMachine(collectionData.getMachine());
		CollectionClassificationMap matchedMap = findMatches(collectionData, existingCollections);
		return matchedMap;
	}
	
	
	private Machine findMachine(String daiIdentifier, String machineIdentifier) {
		
		return null;
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
				if ( matches == normalizedDetails.size() ) {
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
	
	public DaiMeterActual createActual(int collectionId){
		return null;
	}

}
