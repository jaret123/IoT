package com.elyxor.xeros.model.repository;

import com.elyxor.xeros.model.DaiMeterActual;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Collection;

@Repository
public interface DaiMeterActualRepository extends CrudRepository<DaiMeterActual, Integer> {

    @Query(value = "SELECT * FROM xeros_dai_meter_actual where machine_id = :id AND reading_timestamp >= :start AND reading_timestamp <= :end AND (exception LIKE :exception OR exception IS NULL) order by reading_timestamp desc ", nativeQuery = true)
    public Collection<DaiMeterActual> findByMachineIdAndReadingTimestampBetweenAndExceptionLikeOrNull(@Param("id")Integer id, @Param("start")Timestamp start, @Param("end")Timestamp end, @Param("exception")String exception);

    @Query(value = "SELECT * FROM xeros_dai_meter_actual where machine_id = :id AND reading_timestamp >= :start AND reading_timestamp <= :end AND (exception REGEXP :exception) order by reading_timestamp desc ", nativeQuery = true)
    public Collection<DaiMeterActual> findByMachineIdAndReadingTimestampBetweenAndExceptionRegexp(@Param("id")Integer id, @Param("start")Timestamp start, @Param("end")Timestamp end, @Param("exception")String exception);

    @Query(value = "SELECT * FROM xeros_dai_meter_actual where machine_id = :id AND reading_timestamp >= :start AND reading_timestamp <= :end order by reading_timestamp desc ", nativeQuery = true)
    public Collection<DaiMeterActual> findByMachineIdAndReadingTimestampBetween(@Param("id")Integer id, @Param("start")Timestamp start, @Param("end")Timestamp end);

    @Query(value = "SELECT * FROM xeros_dai_meter_actual where machine_id = :id AND reading_timestamp >= :start AND reading_timestamp <= :end AND exception IS NULL order by reading_timestamp desc ", nativeQuery = true)
    public Collection<DaiMeterActual> findByMachineIdAndReadingTimestampBetweenAndExceptionIsNull(Integer id, Timestamp start, Timestamp end);
}
