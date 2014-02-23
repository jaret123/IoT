package com.elyxor.xeros.model.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.elyxor.xeros.model.DaiMeterCollection;

@Repository
public interface DaiMeterCollectionRepository extends CrudRepository<DaiMeterCollection, Integer> {

}
