package com.elyxor.xeros.model.repository;

import com.elyxor.xeros.model.Cycle;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface CycleRepository extends CrudRepository<Cycle, Integer> {
    public final static String FIND_WITH_EXCEPTION = "SELECT cycle FROM CYCLE c LEFT JOIN c.daiMeterActual actual WHERE actual.exception like :exception";

    @Query(value = "SELECT * FROM xeros_cycle where machine_id = :machineId AND reading_timestamp >= :cycleStart AND reading_timestamp <= :cycleEnd", nativeQuery = true)
    public List<Cycle> findCyclesForCycleTime(@Param("machineId") Integer machineId, @Param("cycleStart") Timestamp cycleStart, @Param("cycleEnd") Timestamp cycleEnd);

    public List<Cycle> findByMachineIdAndReadingTimestampBetween(Integer machineId, Timestamp cycleStart, Timestamp cycleEnd);

    public List<Cycle> findByMachineIdAndReadingTimestampBetweenAndDaiMeterActualExceptionLike(Integer id, Timestamp start, Timestamp end, String string);

    public List<Cycle> findByMachineIdAndReadingTimestampBetweenAndDaiMeterActualExceptionNotLike(Integer id, Timestamp start, Timestamp end, String string);
}