package com.elyxor.xeros.model.repository;

import com.elyxor.xeros.model.Status;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatusRepository extends CrudRepository<Status, Integer> {
    @Query(value = "SELECT * FROM xeros_status WHERE machine_id = :machineId ORDER BY status_id desc LIMIT 1", nativeQuery = true)
    public Status findByMachineId(@Param("machineId") int machineId);

    @Query(value = "SELECT * FROM xeros_status WHERE machine_id = :machineId ORDER BY time_stamp desc LIMIT :limit", nativeQuery = true)
    public List<Status> findHistoryByMachineIdWithLimit(@Param("machineId") int machineId, @Param("limit") int limit);

    @Query(value = "SELECT * FROM xeros_status WHERE machine_id = :machineId ORDER BY time_stamp desc", nativeQuery = true)
    public List<Status> findHistoryByMachine(@Param("machine") int machineId);

    @Query public List<Status> findByMachineIdOrderByTimestampDesc(int machineId);
}
