package com.elyxor.common.mongo;

import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.elyxor.common.db.auth.IDataStoreAuthProvider;
import com.elyxor.common.dao.UserDAO;
import com.elyxor.common.utils.ClassloaderUtil;

public class MongoDBManager {

	public final String DB_NAME = "elyxor";

	private Logger _log;
	//
	//	private static MongoDBManager mInstance;
	private final Morphia _morphia;
	private final Mongo _mongoConnection;
	private final DB _mongoDB;
	private final Datastore _datastore;
    private String _dbName = DB_NAME;

	private UserDAO _userDAO;

	private IDataStoreAuthProvider _dbAuthProvider;

	public MongoDBManager(String dbAuthProviderClassName, final Logger log) throws UnknownHostException, MongoException {

		_log = log;

		Object pvdr = (ClassloaderUtil.loadClass(this.getClass().getClassLoader(), dbAuthProviderClassName, log));

		if (null == pvdr) {
			if (null != _log) {
				log.error("Failed to load auth provider: " + dbAuthProviderClassName);
			}
			throw new RuntimeException("Failed to load auth provider to: " + dbAuthProviderClassName);
		}

		_dbAuthProvider = (IDataStoreAuthProvider) pvdr;

		if (null == _dbAuthProvider) {
			if (null != _log) {
				_log.error("Failed to convert auth provider to: " + dbAuthProviderClassName);
			}
			throw new RuntimeException("Failed to convert auth provider to: " + dbAuthProviderClassName);
		}

        _dbName = _dbAuthProvider.getDBName();
		_morphia = new Morphia();
		_mongoConnection = new Mongo(_dbAuthProvider.getHost(), _dbAuthProvider.getPort());
		_mongoDB = _mongoConnection.getDB(_dbName);

		boolean auth = false;
		
		try {
			auth = _mongoDB.authenticate(_dbAuthProvider.getUsername(), _dbAuthProvider.getPassword());
		} catch (RuntimeException re) {
			// try again because sometimes we get socket exceptions in practice with connection resetting during
			// login with MongoDB on same box.
			_log.warn("Caught exception trying to authenticate. Trying again. 1st Error: " + re.getLocalizedMessage(), re);
			auth = _mongoDB.authenticate(_dbAuthProvider.getUsername(), _dbAuthProvider.getPassword());
		}

		if (!auth) {
            _dbName = DB_NAME;
			// failed to authenticate! Let's try try again with the default DB
			_datastore = _morphia.createDatastore(_mongoConnection, _dbName, _dbAuthProvider.getUsername(), _dbAuthProvider.getPassword());
		} else {
			_datastore = _morphia.createDatastore(_mongoConnection, _dbName);
		}

        init();
	}

	public void close() {
	}

	/**
	 * This is the default implementation. Should be called when there is no auth!
	 * 
	 * @throws UnknownHostException
	 * @throws MongoException
	 */
	public MongoDBManager(final Logger log) throws UnknownHostException, MongoException {

        _log = log;
		_morphia = new Morphia();
		_mongoConnection = new Mongo("localhost");
		_mongoDB = _mongoConnection.getDB(_dbName);
		_datastore = _morphia.createDatastore(_mongoConnection, _dbName);

        init();
	}

    private void init() {

    //Morphia to scan a package, and map all classes found in that package
		_morphia.mapPackage("com.elyxor.common.document");
		_datastore.ensureCaps(); //creates capped collections from @Entity 
		_datastore.ensureIndexes(); //creates indexes from @Index annotations in your entities

		_userDAO = new UserDAO(_mongoConnection, _morphia, _dbName);
		_userDAO.setLogger(_log);
    }
    
	public Morphia getMorphia() {
		return _morphia;
	}

	public Mongo getMongoConnection() {
		return _mongoConnection;
	}

	public DB getMongoDB() {
		return _mongoDB;
	}

	public Datastore getDatastore() {
		return _datastore;
	}

	public UserDAO getUserDAO() {
		return _userDAO;
	}
}
