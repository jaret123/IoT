package com.elyxor.xeros.model.repository;

import com.elyxor.xeros.model.Status;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatusRepository extends CrudRepository<Status, Integer> {
    @Query(value = "SELECT * FROM xeros_status WHERE machine_id = :machineId ORDER BY timestamp desc LIMIT 1", nativeQuery = true)
    public Status findByMachineId(@Param("machineId") int machineId);

    @Query(value = "SELECT * FROM xeros_status WHERE machine_id = :machineId ORDER BY time_stamp desc, machine_id desc LIMIT 100", nativeQuery = true)
    public List<Status> findHistoryByMachineId(@Param("machineId") int machineId);
}
