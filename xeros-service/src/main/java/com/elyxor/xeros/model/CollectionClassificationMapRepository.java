package com.elyxor.xeros.model;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectionClassificationMapRepository extends CrudRepository<CollectionClassificationMap, Integer> {
	@Query public List<CollectionClassificationMap> findByMachine(Machine machine);
}
