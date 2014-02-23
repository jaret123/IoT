package com.elyxor.xeros.model.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.elyxor.xeros.model.CollectionClassificationMap;
import com.elyxor.xeros.model.CollectionClassificationMapDetail;

@Repository
public interface CollectionClassificationMapDetailRepository extends CrudRepository<CollectionClassificationMapDetail, Integer> {
	@Query public List<CollectionClassificationMapDetail> findByCollectionClassificationMap(CollectionClassificationMap collectionMap);
}
