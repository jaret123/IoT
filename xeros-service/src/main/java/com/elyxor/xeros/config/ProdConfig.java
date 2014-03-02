package com.elyxor.xeros.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

import com.elyxor.xeros.ApplicationConfig;

@Configuration
@Profile("prod")
@PropertySource({"classpath:/default.properties", "classpath:/prod.properties"})
public class ProdConfig extends ApplicationConfig {

}
