package com.elyxor.common.dao;

import org.bson.types.ObjectId;

import org.apache.log4j.Logger;

import java.util.List;

import com.google.code.morphia.Morphia;
import com.google.code.morphia.dao.BasicDAO;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.code.morphia.query.UpdateResults;
import com.mongodb.Mongo;
import com.mongodb.WriteConcern;
import com.elyxor.common.document.User;

public class UserDAO extends BasicDAO<User, ObjectId> {

	public UserDAO(Mongo mongo, Morphia morphia, String dbName) {
		super(mongo, morphia, dbName);
	}

	public User findById(long id) {
		ObjectId massageToObjectId = ObjectId.massageToObjectId(String.valueOf(id));
		return findById(massageToObjectId);
	}

	public User findById(ObjectId id) {
		return ds.get(User.class, id);
	}

	public User findByTwitterName(String twitterName) {
		Query<User> findQuery = ds.createQuery(User.class).field("twitterName").equal(twitterName);
		if (null == findQuery || findQuery.asList().isEmpty()) {
			return null;
		} else {
			return findQuery.asList().iterator().next();
		}
	}

    public List<User> findWithLimitAndOffset(int limit, int offset, String sortField) {
    
		Query<User> findQuery = ds.createQuery(User.class);
        if (limit > 0) {
            findQuery = findQuery.limit(limit);
        }
        if (offset > 0) {
            findQuery = findQuery.offset(offset);
        }
        if (null != sortField) {
            findQuery = findQuery.order(sortField);
        }
        
		if (null == findQuery) {
            return null;
        }
        
        List<User> users = findQuery.asList();
        return (users.isEmpty()) ? null : users;
    }
    
    public void removeByTwitterName(String twitterName) {
        
    }
    
	public UpdateResults<User> update(Query<User> query, UpdateOperations<User> ops, boolean createIfMissing) {
		return ds.update(query, ops, createIfMissing, WriteConcern.SAFE);
	}

    public void setLogger(final Logger log) {
    }
}
