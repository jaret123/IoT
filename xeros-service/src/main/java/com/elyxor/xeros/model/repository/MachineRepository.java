package com.elyxor.xeros.model.repository;

import com.elyxor.xeros.model.Machine;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MachineRepository extends CrudRepository<Machine, Integer> {	
	@Query List<Machine> findByDaiDaiIdentifierAndMachineIdentifier(String daiIdentifier, String machineIdentifier);
    @Query Machine findById(int machineId);
    @Query(value = "SELECT machine_id FROM xeros_machine", nativeQuery = true)
    List<Integer> findAllMachineIds();
}
