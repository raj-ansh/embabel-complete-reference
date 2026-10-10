package com.northwind.springbasics.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.time.Clock;

/** Tells the container where to find components, and defines beans it cannot discover itself. */
@Configuration
@ComponentScan(basePackages = "com.northwind.springbasics")
@EnableAspectJAutoProxy
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
