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
import com.elyxor.xeros.model.CollectionClassificationMapDetailRepository;
import com.elyxor.xeros.model.CollectionClassificationMapRepository;
import com.elyxor.xeros.model.DaiMeterCollection;
import com.elyxor.xeros.model.DaiMeterCollectionDetail;
import com.elyxor.xeros.model.DaiMeterCollectionDetailRepository;
import com.elyxor.xeros.model.DaiMeterCollectionRepository;

@Transactional
@Service
public class DaiCollectionMatcher {

	private static Logger logger = LoggerFactory.getLogger(DaiCollectionMatcher.class);
	
	@Autowired DaiMeterCollectionRepository daiMeterCollectionRepo;
	@Autowired DaiMeterCollectionDetailRepository daiMeterCollectionDetailRepo;
	@Autowired CollectionClassificationMapRepository collectionClassificationMapRepo;
	@Autowired CollectionClassificationMapDetailRepository collectionClassificationMapDetailRepo;
	
	public int match(int collectionId) {
		CollectionClassificationMap matchedMap = this.match(daiMeterCollectionRepo.findOne(collectionId));
		if (matchedMap!=null) {
			return matchedMap.getId();
		}
		return 0;
	}
	
	public CollectionClassificationMap match(DaiMeterCollection collectionData) {
		CollectionClassificationMap matchedMap = null;
		// Collection<CollectionClassificationMap> existingCollections = this.collectionClassificationMapRepo.findByMachine(collectionData.getMachine());
		Iterable<CollectionClassificationMap> existingCollections = this.collectionClassificationMapRepo.findAll();
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

}
