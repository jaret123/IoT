package com.elyxor.xeros.model.repository;

import com.elyxor.xeros.model.Cycle;
import com.elyxor.xeros.model.DaiMeterActual;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;


@Repository
public interface CycleRepository extends CrudRepository<Cycle, Integer> {
    @Query(value = "SELECT * FROM xeros_cycle where machine_id = :machineId order by reading_timestamp desc limit 1", nativeQuery = true)
    public Cycle findLastCycleByMachine(@Param("machineId") Integer machineId);

    @Query(value = "SELECT * FROM xeros_cycle where machine_id = :machineId AND reading_timestamp >= :cycleStart AND reading_timestamp <= :cycleEnd", nativeQuery = true)
    public List<Cycle> findCyclesForCycleTime(@Param("machineId") Integer machineId, @Param("cycleStart") Timestamp cycleStart, @Param("cycleEnd") Timestamp cycleEnd);

    public List<Cycle> findByMachineIdAndReadingTimestampBetween(Integer machineId, Timestamp cycleStart, Timestamp cycleEnd);

    public List<Cycle> findByMachineIdAndReadingTimestampBetweenAndDaiMeterActualExceptionLike(Integer id, Timestamp start, Timestamp end, String string);

    @Query(value = "SELECT * FROM xeros_cycle as c LEFT JOIN xeros_dai_meter_actual as ac on c.dai_meter_actual_id = ac.dai_meter_actual_id where c.machine_id = :id AND c.reading_timestamp >= :start AND c.reading_timestamp <= :end AND (ac.exception LIKE :exception OR ac.exception IS NULL) order by c.reading_timestamp desc ", nativeQuery = true)
    public List<Cycle> findByMachineIdAndReadingTimestampBetweenAndExceptionLikeOrNull(@Param("id")Integer id, @Param("start")Timestamp start, @Param("end")Timestamp end, @Param("exception")String exception);

    public List<Cycle> findByMachineIdAndReadingTimestampBetweenAndDaiMeterActualExceptionLikeOrDaiMeterActualExceptionIsNullOrderByReadingTimestampDesc(Integer id, Timestamp start, Timestamp end, String string);

    public List<Cycle> findByMachineIdAndReadingTimestampBetweenAndDaiMeterActualExceptionIsNull(Integer id, Timestamp start, Timestamp end);

    public List<Cycle> findByMachineIdAndReadingTimestampBetweenAndDaiMeterActualExceptionNotLike(Integer id, Timestamp start, Timestamp end, String string);

    @Query(value = "SELECT * FROM xeros_cycle as c LEFT JOIN xeros_dai_meter_actual as ac on c.dai_meter_actual_id = ac.dai_meter_actual_id where c.machine_id = :machineId AND c.reading_timestamp >= :start AND c.reading_timestamp <= :end AND ac.exception REGEXP :exception order by c.reading_timestamp desc", nativeQuery = true)
    public List<Cycle> findByDateMachineAndRegex(@Param("machineId") Integer id, @Param("start") Timestamp start, @Param("end") Timestamp end, @Param("exception") String exception);

    @Query(value = "SELECT * FROM xeros_cycle where dai_meter_actual_id in :actuals order by reading_timestamp desc", nativeQuery = true)
    public List<Cycle> findByDaiMeterActualIn(@Param("actuals") Collection<DaiMeterActual> actuals);

    @Query(value = "SELECT * FROM xeros_cycle where dai_meter_actual_id in :actuals and classification_id regexp :classification order by reading_timestamp desc", nativeQuery = true)
    public List<Cycle> findByDaiMeterActualInAndClassificationRegexp(@Param("actuals") Collection<DaiMeterActual> actuals, @Param("classification") String classification);
    @Query(value = "SELECT * FROM xeros_cycle where dai_meter_actual_id in :actuals and (classification_id regexp :classification OR classification_id is null) order by reading_timestamp desc", nativeQuery = true)
    public List<Cycle> findByDaiMeterActualInAndClassificationRegexpOrNull(@Param("actuals") Collection<DaiMeterActual> actuals, @Param("classification") String classification);
}