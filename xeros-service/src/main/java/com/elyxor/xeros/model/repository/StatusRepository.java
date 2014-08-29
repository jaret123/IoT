package com.elyxor.xeros.model.repository;

import com.elyxor.xeros.model.MachineStatus;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatusRepository extends CrudRepository<MachineStatus, Integer> {
}
