package com.elyxor.xeros.ldcs;

import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppConfiguration {
	
	private static Configuration config = null;
	final static Logger logger = LoggerFactory.getLogger(AppConfiguration.class);
	
	public static Configuration getConfig() {
		Properties properties = new Properties();
		try {
			if ( config == null ) {
				logger.info("loading config from [ldcs.properties]");
				InputStream is = AppConfiguration.class.getClassLoader().getResourceAsStream("ldcs.properties");
				if (is==null) {
					logger.warn("ldcs.properties not found; loading config from [application.properties]");
					is = AppConfiguration.class.getClassLoader().getResourceAsStream("application.properties");
				}
				properties.load(is);
				config = new MapConfiguration(properties);
			}
		} catch (Exception ex) {
			logger.error("Failed to build configuration", ex);
		}
		return config;
	}
	
	public static String getLocalPath(){
		return getConfig().getString("localpath");
	}
	
	public static Long getFileLockWait(){
		return getConfig().getLong("file_lock_wait_ms", 10000);
	}
	
	public static String getArchivePath() {
		return getConfig().getString("archivepath");
	}
	
	public static String getServiceUrl() {
		return getConfig().getString("serviceurl");
	}
	
	public static String getLocationId() {
		return getConfig().getString("location_id", "none");
	}
	
	public static String getFilePattern() {
		return getConfig().getString("filepattern");
	}
	
	public static String getDaiName() {
		return getConfig().getString("dainame");
	}
	
	public static Boolean getWaterOnly() {
		return getConfig().getBoolean("water_only");
	}
}
