package com.northwind.support;

import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.northwind.support.domain.SupportDomain.SupportRequest;
import com.northwind.support.domain.SupportDomain.SupportRequest.Category;
import com.northwind.support.domain.SupportDomain.SupportRequest.Urgency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureMockMvc
class SupportApiIntegrationTest extends EmbabelMockitoIntegrationTest {

    @Autowired
    MockMvcTester mvc;

    private void customerAsks(Category category, String order, Urgency urgency) {
        whenCreateObject(p -> p.contains("<message>"), SupportRequest.class)
                .thenReturn(new SupportRequest(category, order, null, "Where is my order?", urgency));
    }

    @Test
    void answersAnOrderStatusQuestionFromRealFacts() {
        customerAsks(Category.ORDER_STATUS, "NW-10442", Urgency.NORMAL);
        whenGenerateText(p -> p.contains("SHIPPED") && p.contains("Answer the question directly"))
                .thenReturn("Hi Asha, your order NW-10442 has shipped and should arrive on 9 October.");

        assertThat(mvc.post().uri("/api/support").header("X-Customer-Id", "C-1001")
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"message":"Where is NW-10442?"}"""))
                .hasStatusOk()
                .bodyJson().satisfies(json -> {
                    assertThat(json).extractingPath("$.escalatedToHuman").isEqualTo(false);
                    assertThat(json).extractingPath("$.message").asString().contains("shipped");
                });
    }

    @Test
    void anotherCustomersOrderIsNeverRevealed() {
        customerAsks(Category.ORDER_STATUS, "NW-10442", Urgency.NORMAL);    // Asha's order, asked by Mateo
        whenGenerateText(p -> p.contains("No order NW-10442 was found"))
                .thenReturn("Hi Mateo, I couldn't find that order on your account.");

        assertThat(mvc.post().uri("/api/support").header("X-Customer-Id", "C-1002")
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"message":"Where is NW-10442?"}"""))
                .hasStatusOk();

        verifyGenerateText(p -> !p.contains("SHIPPED") && !p.contains("189.00"));
    }

    @Test
    void billingQuestionsOpenATicket() {
        customerAsks(Category.BILLING, null, Urgency.LOW);
        whenGenerateText(p -> p.contains("specialist will follow up"))
                .thenReturn("Hi Mateo, a specialist will follow up within one business day.");

        assertThat(mvc.post().uri("/api/support").header("X-Customer-Id", "C-1002")
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"message":"I was charged twice"}"""))
                .hasStatusOk()
                .bodyJson().satisfies(json -> {
                    assertThat(json).extractingPath("$.escalatedToHuman").isEqualTo(true);
                    assertThat(json).extractingPath("$.ticketReference").asString().startsWith("TCK-");
                });
    }
}
