package com.elyxor.xeros.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

import com.elyxor.xeros.ApplicationConfig;

@Configuration
@Profile("qa")
@PropertySource({"classpath:/default.properties", "classpath:/qa.properties"})
public class QAConfig extends ApplicationConfig {

}
