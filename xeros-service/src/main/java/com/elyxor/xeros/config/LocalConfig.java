package com.elyxor.xeros.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

import com.elyxor.xeros.ApplicationConfig;

@Configuration
@Profile("local")
@PropertySource({"classpath:/default.properties", "classpath:/local.properties"})
public class LocalConfig extends ApplicationConfig {

}
