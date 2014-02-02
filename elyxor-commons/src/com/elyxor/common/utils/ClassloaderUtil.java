package com.elyxor.common.utils;

import org.apache.log4j.Logger;

public class ClassloaderUtil {

	public static Object loadClass(String className, Logger logger) {
    	return loadClass(ClassloaderUtil.class.getClassLoader(), className, logger);
	}

	public static Object loadClass(ClassLoader cl, String className, Logger logger) {
		try {
			logger.debug("Attempting to load class: " + className);
	        Class<?> myClass = cl.loadClass(className);
	 
	        if (null != myClass) {
		        logger.debug("Loaded class: " + myClass.getName()); } 
	        else {
	        	logger.debug("FAILED to load class: " + className);
	        }
	        Object whatInstance = myClass.newInstance();

	        if (null == whatInstance) {
		        logger.debug("Failed to instantiate class: " + myClass.getName());
	        }

	        return whatInstance;
	    } catch (SecurityException e) {
	    	logger.error(e);
	    } catch (IllegalArgumentException e) {
	        logger.error(e);
	    } catch (ClassNotFoundException e) {
	       	logger.error(e);
        } catch (InstantiationException e) {
            logger.error(e);
        } catch (IllegalAccessException e) {
            logger.error(e);
        }
		
		return null;
    }

}
