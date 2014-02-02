package com.elyxor.common.db.auth;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.prefs.Preferences;

import org.apache.log4j.Logger;

public class PreferencesDBAuthProvider implements IDataStoreAuthProvider {

	private static final Logger log = Logger.getLogger(PreferencesDBAuthProvider.class);

	private static final String PREF_DB_HOST = "MONGO_DB_HOST";
	private static final String PREF_DB_PORT = "MONGO_DB_PORT";
	private static final String PREF_DB_NAME = "MONGO_DB_NAME";
	private static final String PREF_DB_USER = "MONGO_DB_USER";
	private static final String PREF_DB_PASS = "MONGO_DB_PASS";

	public PreferencesDBAuthProvider() {
		String str = System.getProperty("user.home") + File.separatorChar + "elyxor.props";
		this.importFromFile(str);
	}

	@Override
	public String getHost() {
		return _host;
	}

	private String _host;

	@Override
	public int getPort() {
		return _port;
	}

	private int _port;

	@Override
	public String getDBName() {
		return _dbName;
	}
	private String _dbName;

	@Override
	public String getUsername() {
		return _username;
	}
	private String _username;

	@Override
	public char[] getPassword() {
		char[] password = _password.toCharArray();
		return password;
	}
	private String _password;

	public void importFromFile(String path) {

		log.debug("Importing db auth preferences from " + path);
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(path);
			Preferences.importPreferences(fis);
		} catch (Exception e) {
			log.error("Failed to import preferences from " + path, e);
		} finally {
			try { fis.close(); } catch (Exception e) { }
		}

		Preferences pref = Preferences.userNodeForPackage(getClass());
		_host = pref.get(PREF_DB_HOST, "127.0.0.1");
		_port = pref.getInt(PREF_DB_PORT, 27017);
		_dbName = pref.get(PREF_DB_NAME, "rapid7db");
		_username = pref.get(PREF_DB_USER, "");
		_password = pref.get(PREF_DB_PASS, "");
	}
	
	public void writeDefaultPrefs(String path) {
		Preferences pref = Preferences.userNodeForPackage(getClass());
		pref.put(PREF_DB_HOST, "127.0.0.1");
		pref.putInt(PREF_DB_PORT, 27017);
		pref.put(PREF_DB_NAME, "rapid7db");
		pref.put(PREF_DB_USER, "");
		pref.put(PREF_DB_PASS, "");

		try {
	          OutputStream osTree =
	             new BufferedOutputStream(
	              new FileOutputStream(path));
	          pref.exportSubtree(osTree);
	          osTree.close();
	      } catch (Exception e) {}
	}
}
