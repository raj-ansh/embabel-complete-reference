package com.northwind.tenancy;

import com.northwind.tenancy.knowledge.TenantKnowledge;
import com.northwind.tenancy.tenant.Tenants;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** P13: NorthWind's support-assistant platform, serving several retailers from one deployment. */
@SpringBootApplication
@EnableConfigurationProperties(Tenants.class)
public class TenancyApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenancyApplication.class, args);
    }

    @Bean
    ApplicationRunner ingestTenantDocuments(TenantKnowledge knowledge) {
        return args -> knowledge.ingestAll();
    }
}
