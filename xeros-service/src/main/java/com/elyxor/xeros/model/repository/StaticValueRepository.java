package com.elyxor.xeros.model.repository;

import com.elyxor.xeros.model.StaticValue;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StaticValueRepository extends CrudRepository<StaticValue, Integer> {
    public StaticValue findByName(String name);
}