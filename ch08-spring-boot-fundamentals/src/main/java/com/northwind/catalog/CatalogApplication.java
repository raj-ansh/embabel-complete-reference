package com.northwind.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication          // = @Configuration + @EnableAutoConfiguration + @ComponentScan
@ConfigurationPropertiesScan    // finds @ConfigurationProperties records such as CatalogProperties
public class CatalogApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogApplication.class, args);
    }
}
