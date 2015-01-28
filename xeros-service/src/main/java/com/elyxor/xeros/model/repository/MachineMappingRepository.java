package com.elyxor.xeros.model.repository;

import com.elyxor.xeros.model.MachineMapping;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MachineMappingRepository extends CrudRepository<MachineMapping, Integer> {
    public List<MachineMapping> findByDaiId(Integer daiId);
    public List<MachineMapping> findByEkId(Integer ekId);
}