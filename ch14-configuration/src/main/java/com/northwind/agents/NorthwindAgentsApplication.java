package com.northwind.agents;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NorthwindAgentsApplication {

    public static void main(String[] args) {
        SpringApplication.run(NorthwindAgentsApplication.class, args);
    }
}
