package com.elyxor.xeros.ldcs;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

public class AppConfiguration {
	
	private static Configuration config = null;
	private static Logger logger = LoggerFactory.getLogger(AppConfiguration.class);
	
	public static Configuration getConfig() {
		Properties properties = new Properties();
		InputStream is = null;
		try {
			if ( config == null ) {
				String propertiesFile = System.getProperty("properties.file", "ldcs.properties");
				if (StringUtils.isNotBlank(propertiesFile)) {
					logger.info("loading config from [{}]", propertiesFile);
					is = AppConfiguration.class.getClassLoader().getResourceAsStream(propertiesFile);
					if (is==null) {
						logger.warn("[{}] not found", propertiesFile);
					} else {
						logger.warn("properties loaded from [{}]", propertiesFile);
					}
				}
				if (is==null) {
					logger.info("loading config from [application.properties]");
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
	
	public static String getServiceUrl() {
		return getConfig().getString("serviceurl");
	}
	
	public static String getDaiName() {
		return getConfig().getString("dainame");
	}
	
	public static Integer getWaterOnly() {
		return getConfig().getInteger("water_only", 0);
	}

    public static Integer getPortConfig() {return getConfig().getInteger("machine_config", 0);}

    public static String getPortType() {return getConfig().getString("port_type");}

    public static Boolean getThingWorx() {return getConfig().getBoolean("thingworx", false);}

    public static Integer getReliagatePortCount() {return getConfig().getInteger("reliagate_ports", 0);}
}
