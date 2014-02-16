package com.elyxor.xeros.ldcs;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppConfiguration {
	
	private static Configuration config = null;
	private static Logger logger = LoggerFactory.getLogger(AppConfiguration.class);
	
	public static Configuration getConfig() {
		try {
		if ( config == null ) {
			config = new PropertiesConfiguration("application.properties");
		}
		} catch (Exception ex) {
			logger.error("Failed to build configuration", ex);
		}
		return config;
	}
	
	public static String getLocalPath(){
		return getConfig().getString("localpath");
	}
	
	public static String getArchivePath() {
		return getConfig().getString("archivepath");
	}
	
	public static String getServiceUrl() {
		return getConfig().getString("serviceurl");
	}
	
	public static String getFilePattern() {
		return getConfig().getString("filepattern");
	}
}
