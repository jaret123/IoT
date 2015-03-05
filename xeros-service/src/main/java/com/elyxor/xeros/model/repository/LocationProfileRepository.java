package com.elyxor.xeros.model.repository;

import com.elyxor.xeros.model.LocationProfile;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by will on 3/1/15.
 */

@Repository
public interface LocationProfileRepository extends CrudRepository<LocationProfile, Integer> {

}
