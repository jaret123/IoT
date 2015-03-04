package com.elyxor.xeros.model.repository;

import com.elyxor.xeros.model.XerosLocalStaticValue;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface XerosLocalStaticValueRepository extends CrudRepository<XerosLocalStaticValue, Integer> {
    @Query public XerosLocalStaticValue findByClassification(Integer classification);
}