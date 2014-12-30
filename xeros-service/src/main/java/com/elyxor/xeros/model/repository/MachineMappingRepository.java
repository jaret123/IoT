package com.elyxor.xeros.model.repository;

import com.elyxor.xeros.model.MachineMapping;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MachineMappingRepository extends CrudRepository<MachineMapping, Integer> {
}