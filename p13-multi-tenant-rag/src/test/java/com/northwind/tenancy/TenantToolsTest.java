package com.northwind.tenancy;

import com.northwind.tenancy.service.TenantSupportService;
import com.northwind.tenancy.tenant.TenantContext;
import com.northwind.tenancy.tenant.Tenants.Plan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 104: one order tool for every tenant, scoped by the tenant and user in tool call context. */
@SpringBootTest
@ActiveProfiles("lab")
class TenantToolsTest {

    static final TenantContext ASHA = new TenantContext("acme-outdoors", "Acme Outdoors", Plan.BASIC, "asha", "");
    static final TenantContext BEN = new TenantContext("acme-outdoors", "Acme Outdoors", Plan.BASIC, "ben", "");
    static final TenantContext CHLOE = new TenantContext("peak-gear", "Peak Gear", Plan.PREMIUM, "chloe", "");

    @Autowired TenantSupportService support;

    private String ask(TenantContext who, String order) {
        String text = support.answer(who, "Where is my order " + order + "?").text();
        System.out.println(who.userId() + " (" + who.tenantId() + ") asks about " + order + ": " + text);
        return text;
    }

    @Test
    void ordersAreScopedByTenantAndCustomer() {
        assertThat(ask(ASHA, "NW-20001")).contains("SHIPPED");
        assertThat(ask(BEN, "NW-20001")).contains("No order");           // same tenant, another customer
        assertThat(ask(CHLOE, "NW-20001")).contains("No order");         // another tenant
        assertThat(ask(ASHA, "NW-30001")).contains("No order");          // Peak Gear's order
    }
}
