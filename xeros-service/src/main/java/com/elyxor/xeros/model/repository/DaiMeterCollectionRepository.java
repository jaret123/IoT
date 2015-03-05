package com.elyxor.xeros.model.repository;

import com.elyxor.xeros.model.DaiMeterCollection;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DaiMeterCollectionRepository extends CrudRepository<DaiMeterCollection, Integer> {
    public DaiMeterCollection findByDaiMeterActualId(int daiMeterActualId);
}

