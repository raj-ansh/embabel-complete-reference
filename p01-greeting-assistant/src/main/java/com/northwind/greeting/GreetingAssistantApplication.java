package com.northwind.greeting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class GreetingAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(GreetingAssistantApplication.class, args);
    }
}
