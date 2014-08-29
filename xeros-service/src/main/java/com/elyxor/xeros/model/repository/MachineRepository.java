package com.elyxor.xeros.model.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.elyxor.xeros.model.Machine;

@Repository
public interface MachineRepository extends CrudRepository<Machine, Integer> {	
	@Query List<Machine> findByDaiDaiIdentifierAndMachineIdentifier(String daiIdentifier, String machineIdentifier);
    @Query Machine findById(int machineId);
}
