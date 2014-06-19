package com.elyxor.xeros.model.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.elyxor.xeros.model.ActiveDai;

@Repository
public interface ActiveDaiRepository extends CrudRepository<ActiveDai, Integer> {
	@Query List<ActiveDai> findByDaiIdentifier(String daiIdentifier);
}