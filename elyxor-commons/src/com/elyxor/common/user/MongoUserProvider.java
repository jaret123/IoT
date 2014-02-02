package com.elyxor.common.user;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;

import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.elyxor.common.mongo.MongoDBManager;
import com.elyxor.common.document.User;

public class MongoUserProvider implements IUserProvider {

	private final Logger log = Logger.getLogger(MongoUserProvider.class);
	private MongoDBManager _dbManager;
	private String _authProviderClassName;

	public MongoUserProvider(String dbAuthProviderClassName, Logger log) {
		if (null == dbAuthProviderClassName || dbAuthProviderClassName.isEmpty() )
			throw new RuntimeException("missing db auth provider. Cannot proceed");
		
		_authProviderClassName = dbAuthProviderClassName;
	}

    public MongoUserProvider() {
	}

	public boolean loadDBManager() {
		
		if (null != _dbManager) { return true; }
		
		log.info("Configuring mongoDB auth provider: " + _authProviderClassName);
		try {
            if (null != _authProviderClassName) {
                _dbManager = new MongoDBManager(_authProviderClassName, log);
            } else {
                _dbManager = new MongoDBManager(log);
            }
			return true;
		} catch (UnknownHostException e) {
			log.error("MongoDB Host is unknown: " + e.getLocalizedMessage(), e);
		} catch (MongoException e) {
			log.error("MongoException: " + e.getLocalizedMessage(), e);
		}
		return false;
	}
	
	@Override
	public List<User> getUsers() {
		return getUsers(-1, 0);
	}

	@Override
	public List<User> getUsers(int limit, int offset) {

		if (!loadDBManager()) {
			log.error("getUsers() - couldn't load db manager. This should never happen, but alas, it did");
			return null;
		}
		
        List<User> users = _dbManager.getUserDAO().findWithLimitAndOffset(limit, offset, "twitterName");

		return users;
	}

	@Override
	public User getByTwitterName(String twitterName) {

		if (!loadDBManager()) {
			log.error("getByTwitterName() - couldn't load db manager. This should never happen, but alas, it did");
			return null;
		}

		return _dbManager.getUserDAO().findByTwitterName(twitterName);
    }

	@Override
	protected void finalize() throws Throwable {
		super.finalize();

		// Close the connection to free up resources
		try {
			if (null != _dbManager) {				
				_dbManager.close();
			}
		} catch (Exception e) {
			log.error("finalize() - error closing mongo connection", e);
		}
		_dbManager = null;
	}

	@Override
    public void add(String firstName, String lastName, String twitterName) {

    if (!loadDBManager()) {
			log.error("add() - couldn't load db manager. This should never happen, but alas, it did");
		}

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setTwitterName(twitterName);
        
		_dbManager.getUserDAO().save(user);
    }
    

    @Override
    public void remove(String twitterName) {

        if (!loadDBManager()) {
			log.error("add() - couldn't load db manager. This should never happen, but alas, it did");
		}

        User user = getByTwitterName(twitterName);
        if (null != user) {
            _dbManager.getUserDAO().delete(user);
        } else {
            throw new RuntimeException("User not found with twitter name: " + twitterName);
        }
    }
}
