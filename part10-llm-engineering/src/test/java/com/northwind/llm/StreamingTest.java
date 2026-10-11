package com.northwind.llm;

import com.northwind.llm.service.HighlightStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 77: a streamed REST reply, and a stream of typed objects. */
@AutoConfigureMockMvc
class StreamingTest extends LabTest {

    @Autowired MockMvcTester mvc;
    @Autowired HighlightStream highlights;

    @Test
    void serverSentEvents() throws Exception {
        MvcTestResult result = mvc.get().uri("/api/replies/stream?message={m}", "Has my order NW-10442 shipped?")
                .accept(MediaType.TEXT_EVENT_STREAM).exchange();
        String body = result.getResponse().getContentAsString();
        System.out.println("Content-Type: " + result.getResponse().getContentType());
        System.out.println(body);
        assertThat(body).contains("event:chunk").contains("event:done");
    }

    @Test
    void rejectsOversizedInput() {
        assertThat(mvc.get().uri("/api/replies/stream?message={m}", "x".repeat(2_001)).exchange()).hasStatus(400);
    }

    @Test
    void objectStream() {
        List<String> arrivals = highlights.bestSellers(3)
                .map(p -> "received %-9s %s".formatted(p.sku(), p.name()))
                .collectList().block(Duration.ofSeconds(30));
        arrivals.forEach(System.out::println);
        assertThat(arrivals).hasSize(3);
    }
}
