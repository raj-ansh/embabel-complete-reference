package com.northwind.production.events;

import io.opentelemetry.api.GlobalOpenTelemetry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 99: a signed webhook becomes an event; the agent runs asynchronously, once per event. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("lab")
class EventDrivenTest {

    @BeforeAll
    static void resetGlobalOpenTelemetry() {
        GlobalOpenTelemetry.resetForTest();
    }

    @Autowired MockMvcTester mvc;
    @Autowired CarrierWebhookController webhook;
    @Autowired Outbox outbox;

    private int deliver(String body, String signature) {
        return mvc.post().uri("/webhooks/carrier").header("X-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange().getResponse().getStatus();
    }

    @Test
    void failedDeliveryLeadsToAProactiveMessage() throws Exception {
        outbox.clear();
        String body = """
                {"eventId":"evt-7781","orderNumber":"NW-10501","customerId":"C-1001","carrierNote":"Parcel delayed at depot"}""";
        String signature = webhook.sign(body.getBytes(StandardCharsets.UTF_8));

        System.out.println("signed delivery: " + deliver(body, signature));
        System.out.println("same event again: " + deliver(body, signature));
        System.out.println("forged signature: " + deliver(body.replace("NW-10501", "NW-10442"), signature));

        for (int i = 0; i < 100 && outbox.messages().isEmpty(); i++) {
            Thread.sleep(50);
        }
        Thread.sleep(500);                                           // give a duplicate time to show up
        outbox.messages().forEach(m -> System.out.println("outbox: to " + m.customerId() + " about " + m.orderNumber()
                + ": " + m.text()));
        assertThat(outbox.messages()).hasSize(1);
    }
}
