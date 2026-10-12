package com.northwind.platform;

import com.northwind.platform.knowledge.PolicyKnowledge;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.northwind.platform.tenant.Tenants;
import org.springframework.context.annotation.Bean;

/** P15: NorthWind's production-grade agentic platform (the capstone). */
@SpringBootApplication
@EnableConfigurationProperties(Tenants.class)
public class ProductionPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductionPlatformApplication.class, args);
    }

    @Bean
    ApplicationRunner ingestPolicies(PolicyKnowledge policies) {
        return args -> policies.ingest();
    }
}
