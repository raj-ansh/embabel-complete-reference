package com.northwind.tenancy;

import com.northwind.tenancy.knowledge.TenantKnowledge;
import com.northwind.tenancy.service.TenantSupportService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 107: per-tenant metrics, and onboarding and offboarding a tenant's documents. */
@SpringBootTest
@ActiveProfiles("lab")
class TenantOperationsTest {

    @Autowired TenantSupportService support;
    @Autowired TenantKnowledge knowledge;
    @Autowired MeterRegistry meters;

    @Test
    void metricsAreTaggedByTenant() {
        support.answer(TenantIsolationTest.ACME, "What is your returns policy?");
        support.answer(TenantIsolationTest.PEAK, "What is your returns policy?");

        meters.find("northwind.tenant.llm.tokens").counters().stream()
                .map(c -> "tokens " + c.getId().getTag("tenant") + " on " + c.getId().getTag("model") + ": positive "
                        + (c.count() > 0))
                .sorted().forEach(System.out::println);
        assertThat(meters.find("northwind.tenant.llm.cost").tag("tenant", "peak-gear").counter()).isNotNull();
    }

    @Test
    void onboardingAndOffboardingADocument() {
        String uri = "northwind://tenants/acme-outdoors/stove-safety.md";
        knowledge.ingest(uri, "# Acme Outdoors stove safety\n\nNever use a camping stove inside a tent or porch.");
        String before = TenantIsolationTest.search(knowledge.searchFor(TenantIsolationTest.ACME), "stove inside a tent");
        int deleted = knowledge.deleteTenantDocument(uri);
        String after = TenantIsolationTest.search(knowledge.searchFor(TenantIsolationTest.ACME), "stove inside a tent");

        System.out.println("after onboarding, Acme finds the stove page: " + before.contains("stove safety"));
        System.out.println("offboarding deleted " + deleted + " item(s); Acme still finds it: " + after.contains("stove safety"));
        try {
            knowledge.ingest("northwind://shared/stove-safety.md", "# Unowned");
        } catch (IllegalArgumentException e) {
            System.out.println("a document without a tenant is refused: " + e.getMessage());
        }
        assertThat(after).doesNotContain("stove safety");
    }
}
