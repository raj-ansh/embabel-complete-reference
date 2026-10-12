package com.northwind.enterprise;

import com.northwind.enterprise.knowledge.PolicyKnowledge;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** P14: NorthWind's enterprise support platform. */
@SpringBootApplication
public class EnterpriseSupportApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnterpriseSupportApplication.class, args);
    }

    @Bean
    ApplicationRunner ingestPolicies(PolicyKnowledge policies) {
        return args -> policies.ingest();
    }
}
