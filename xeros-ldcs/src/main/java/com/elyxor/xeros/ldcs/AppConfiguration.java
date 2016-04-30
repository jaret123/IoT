package com.elyxor.xeros.ldcs;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class AppConfiguration {
	
	private static Configuration config = null;
	private static Configuration portConfig = null;
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

	public static Configuration getPortListConfig() {
		Configuration portConfig = new MapConfiguration(new HashMap<String, Object>());
		String globalControllerPortListFile = System.getProperty("properties.file", "portlist.properties");
		InputStream input = AppConfiguration.class.getClassLoader().getResourceAsStream("portlist.properties");
		if (input != null) {
			logger.info("Loading port list from [{}]", "portlist.properties");
			Properties props = new Properties();
			try {
				props.load(input);
			} catch (IOException e) {
				e.printStackTrace();
			}
			portConfig = new MapConfiguration(props);
		}
		return portConfig;
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

    public static Boolean isGlobalController() {return getConfig().getBoolean("global", false);}

    public static Boolean getMock() {return getConfig().getBoolean("mock", false);}

    public static String getThingWorxUrl() {return getConfig().getString("thingworx_url", "wss://xeros-prod.cloud.thingworx.com:443/Thingworx/WS");}

    public static String getThingWorxApiKey() {return getConfig().getString("thingworx_key", "253f9e18-9dce-49c0-8cea-028cb51e8729");}

    public static double getDoorLockMin() {return getConfig().getDouble("door_lock_min", .5);}

	public static List<String> getGlobalControllerPortList() {
		List<String> result = new ArrayList<String>();
		Configuration config = getPortListConfig();
		Iterator<String> iterator = config.getKeys();
		while (iterator.hasNext()) {
			String key = iterator.next();
			ArrayList<String> value = (ArrayList<String>) config.getProperty(key);
			result.add(key + "=" + value.get(0) + "," + value.get(1));
		}
		return result;
	}

}
