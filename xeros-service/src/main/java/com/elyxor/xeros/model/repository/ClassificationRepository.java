package com.elyxor.xeros.model.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.elyxor.xeros.model.Classification;

@Repository
public interface ClassificationRepository extends CrudRepository<Classification, Integer> {
}
