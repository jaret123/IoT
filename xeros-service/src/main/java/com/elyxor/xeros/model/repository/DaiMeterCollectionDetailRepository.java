package com.elyxor.xeros.model.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.elyxor.xeros.model.DaiMeterCollection;
import com.elyxor.xeros.model.DaiMeterCollectionDetail;

@Repository
public interface DaiMeterCollectionDetailRepository extends CrudRepository<DaiMeterCollectionDetail, Integer> {
	@Query public List<DaiMeterCollectionDetail> findByDaiMeterCollection(DaiMeterCollection collection);
}


