package com.northwind.agents;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** A normal Spring Boot application. Adding the Embabel starter is what turns on the agent platform. */
@SpringBootApplication
public class NorthwindAgentsApplication {

    public static void main(String[] args) {
        SpringApplication.run(NorthwindAgentsApplication.class, args);
    }
}
