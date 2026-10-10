package com.northwind.purchasing;

import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import com.northwind.purchasing.domain.Purchasing.PurchaseRequest;
import com.northwind.purchasing.domain.Purchasing.RiskNote;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** P9 over HTTP: submit, a forbidden self-approval, then a real approval. */
@AutoConfigureMockMvc
class PurchaseApiTest extends EmbabelMockitoIntegrationTest {

    @Autowired
    MockMvcTester mvc;

    private static String body(MvcTestResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void theApprovalJourneyOverHttp() throws Exception {
        whenCreateObject(p -> p.contains("<request>"), PurchaseRequest.class)
                .thenReturn(new PurchaseRequest("TrailTech", "GPS units", 12, new BigDecimal("249.00"), "Guided hikes"));
        whenCreateObject(p -> p.contains("<purchase"), RiskNote.class)
                .thenReturn(new RiskNote("GPS units for guided hikes.", List.of("No supplier quote attached")));

        MvcTestResult submitted = mvc.post().uri("/api/purchases")
                .header("X-Employee-Id", "E-17").header("X-Department", "Guiding")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\": \"12 GPS units from TrailTech at 249 each for guided hikes\"}")
                .exchange();
        assertThat(submitted).hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("WAITING");
        String id = JsonPath.read(body(submitted), "$.processId");

        MvcTestResult selfApproval = mvc.post().uri("/api/purchases/{id}/decision", id)
                .header("X-Employee-Id", "E-17").header("X-Roles", "budget-holder")
                .contentType(MediaType.APPLICATION_JSON).content("{\"approved\": true}")
                .exchange();
        System.out.println("self-approval: " + body(selfApproval));
        assertThat(selfApproval).hasStatus(403);

        MvcTestResult approval = mvc.post().uri("/api/purchases/{id}/decision", id)
                .header("X-Employee-Id", "M-3").header("X-Roles", "employee,budget-holder")
                .contentType(MediaType.APPLICATION_JSON).content("{\"approved\": true, \"comment\": \"OK\"}")
                .exchange();
        System.out.println("approved: " + body(approval));
        assertThat(approval).hasStatusOk().bodyJson().extractingPath("$.result.status").isEqualTo("ORDERED");
    }
}
