package com.northwind.greeting.web;

import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/** The whole application: web layer, configuration, CustomerDirectory and Embabel, with the model stubbed. */
@AutoConfigureMockMvc
class GreetingApiIntegrationTest extends EmbabelMockitoIntegrationTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void greetsAKnownCustomer() {
        whenGenerateText(prompt -> prompt.contains("Asha") && prompt.contains("happy birthday"))
                .thenReturn("Happy birthday, Asha! May your next trail be your best yet.");

        assertThat(mvc.get().uri("/api/customers/C-1001/greeting?occasion=BIRTHDAY"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.greeting").asString().startsWith("Happy birthday, Asha!");

        verifyGenerateText(prompt -> prompt.contains("GOLD") && prompt.contains("at most 40 words"));
    }

    @Test
    void unknownCustomerIs404AndNoModelIsCalled() {
        assertThat(mvc.get().uri("/api/customers/C-9999/greeting"))
                .hasStatus(HttpStatus.NOT_FOUND);

        verifyNoInteractions();                       // the model was never called
    }
}
