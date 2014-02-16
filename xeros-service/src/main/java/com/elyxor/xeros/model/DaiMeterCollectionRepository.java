package com.elyxor.xeros.model;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DaiMeterCollectionRepository extends CrudRepository<DaiMeterCollection, Long> {

}
