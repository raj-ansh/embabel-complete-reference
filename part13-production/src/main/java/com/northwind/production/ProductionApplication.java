package com.northwind.production;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Part XIII: NorthWind's issue-resolution service, built for production. */
@SpringBootApplication
public class ProductionApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductionApplication.class, args);
    }
}
