package com.northwind.tenancy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.northwind.tenancy.security.LabTokens;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapters 103 and 107: tenants are resolved from tokens, and everything the API does is scoped by them. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("lab")
class TenantApiTest {

    @Autowired MockMvcTester mvc;
    @Autowired LabTokens tokens;

    private MvcTestResult ask(String token, String json) {
        var request = mvc.post().uri("/api/support").contentType(MediaType.APPLICATION_JSON).content(json);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return request.exchange();
    }

    private static String shorten(MvcTestResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return result.getResponse().getStatus() + (body.isEmpty() ? "" : " " + body.substring(0, Math.min(110, body.length())));
    }

    @Test
    void tenantComesFromTheToken() throws Exception {
        String question = "{\"question\":\"What is your returns policy?\"}";
        System.out.println("no token:            " + shorten(ask(null, question)));
        var acme = ask(tokens.tokenFor("asha", "acme-outdoors", "support"), question);
        System.out.println("Acme Outdoors token: " + shorten(acme));
        var peak = ask(tokens.tokenFor("chloe", "peak-gear", "support"), question);
        System.out.println("Peak Gear token:     " + shorten(peak));
        System.out.println("unknown tenant:      " + shorten(ask(tokens.tokenFor("eve", "ghost-shop", "support"), question)));
        System.out.println("suspended tenant:    " + shorten(ask(tokens.tokenFor("dan", "old-camp", "support"), question)));
        assertThat(acme.getResponse().getContentAsString()).contains("30 days");
        assertThat(peak.getResponse().getContentAsString()).contains("14 days");
    }

    @Test
    void aTenantInTheRequestBodyIsIgnored() throws Exception {
        var smuggled = ask(tokens.tokenFor("asha", "acme-outdoors", "support"),
                "{\"question\":\"What is your returns policy?\",\"tenant\":\"peak-gear\"}");
        String body = smuggled.getResponse().getContentAsString();
        System.out.println("Acme token, body says peak-gear: answered from Acme's policy: " + body.contains("30 days")
                + ", from Peak Gear's: " + body.contains("14 days"));
        assertThat(body).contains("30 days").doesNotContain("14 days");
    }

    @Test
    void usageIsForAdministratorsOfTheirOwnTenant() throws Exception {
        var customer = mvc.get().uri("/api/admin/usage")
                .header("Authorization", "Bearer " + tokens.tokenFor("asha", "acme-outdoors", "support")).exchange();
        var admin = mvc.get().uri("/api/admin/usage")
                .header("Authorization", "Bearer " + tokens.tokenFor("ops-1", "acme-outdoors", "support tenant:admin")).exchange();
        System.out.println("usage as a customer:     " + customer.getResponse().getStatus());
        System.out.println("usage as Acme's admin:   " + admin.getResponse().getStatus() + " "
                + admin.getResponse().getContentAsString().replaceAll("\"spentTodayUsd\":[0-9.E-]+", "\"spentTodayUsd\":<amount>"));
        assertThat(customer).hasStatus(403);
        assertThat(admin.getResponse().getContentAsString()).contains("\"tenant\":\"acme-outdoors\"");
    }

    @Test
    void logLinesNameTheTenant() {
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        var root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.addAppender(appender);
        try {
            ask(tokens.tokenFor("asha", "acme-outdoors", "support"), "{\"question\":\"How do I clean my tent?\"}");
        } finally {
            root.detachAppender(appender);
        }
        var agentLines = appender.list.stream().filter(e -> e.getLoggerName().equals("Embabel")).toList();
        long naming = agentLines.stream().filter(e -> "acme-outdoors".equals(e.getMDCPropertyMap().get("tenant"))).count();
        System.out.println("Embabel log lines during the request all name the tenant: " + (naming == agentLines.size()));
        assertThat(naming).isEqualTo(agentLines.size()).isPositive();
    }
}
