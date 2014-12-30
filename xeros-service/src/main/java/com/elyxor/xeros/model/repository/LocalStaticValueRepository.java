package com.elyxor.xeros.model.repository;

import com.elyxor.xeros.model.Classification;
import com.elyxor.xeros.model.LocalStaticValue;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalStaticValueRepository extends CrudRepository<LocalStaticValue, Integer> {
    public LocalStaticValue findByClassification(Classification classification);
}