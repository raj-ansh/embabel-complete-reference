package com.northwind.returns.web;

import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.northwind.returns.domain.CustomerReply;
import com.northwind.returns.domain.ReturnRequest;
import com.northwind.returns.domain.ReturnRequest.ReturnReason;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureMockMvc
class ReturnsApiIntegrationTest extends EmbabelMockitoIntegrationTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    ReturnsFacade facade;

    @Test
    void freeTextMessageIsHandledEndToEnd() {
        whenCreateObject(p -> p.contains("<message>"), ReturnRequest.class)
                .thenReturn(new ReturnRequest("NW-10442", "TENT-2P", ReturnReason.DAMAGED, "snapped"));
        whenCreateObject(p -> p.contains("Decision: APPROVED"), CustomerReply.class)
                .thenReturn(new CustomerReply("Your return is approved", "Hi Asha…"));

        assertThat(mvc.post().uri("/api/returns/messages").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"message":"My tent pole snapped. Order NW-10442."}"""))
                .hasStatusOk()
                .bodyJson().satisfies(json -> {
                    assertThat(json).extractingPath("$.status").isEqualTo("COMPLETED");
                    assertThat(json).extractingPath("$.reply.subject").isEqualTo("Your return is approved");
                    assertThat(json).extractingPath("$.processId").asString().isNotBlank();
                });
    }

    @Test
    void formSubmissionUsesOnlyOneModelCall() {
        whenCreateObject(p -> p.contains("Decision: REJECTED"), CustomerReply.class)
                .thenReturn(new CustomerReply("About your return", "Hi Mateo…"));

        assertThat(mvc.post().uri("/api/returns/form").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"orderNumber":"NW-09001","sku":"BOOT-W9","reason":"CHANGED_MIND"}"""))
                .hasStatusOk()
                .bodyJson().extractingPath("$.reply.subject").isEqualTo("About your return");

        verifyCreateObject(p -> p.contains("Decision: REJECTED"), CustomerReply.class);
        verifyNoMoreInteractions();
    }

    @Test
    void invocationOptionsCarryABudget() {
        var budget = facade.options().getBudget();

        assertThat(budget.getCost()).isEqualTo(0.25);
        assertThat(budget.getActions()).isEqualTo(10);
        assertThat(budget.getTokens()).isEqualTo(20_000);
    }
}
