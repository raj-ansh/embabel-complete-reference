package com.northwind.production.web;

import io.opentelemetry.api.GlobalOpenTelemetry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 97: the HTTP contract, tested through Spring MVC with the lab models. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("lab")
class IssueApiTest {

    @BeforeAll
    static void resetGlobalOpenTelemetry() {
        GlobalOpenTelemetry.resetForTest();
    }

    @Autowired MockMvcTester mvc;

    private MvcTestResult post(String uri, String customer, String json) {
        return mvc.post().uri(uri).header("X-Customer-Id", customer)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private static void show(String label, MvcTestResult result) throws Exception {
        System.out.println(label + ": " + result.getResponse().getStatus() + " "
                + result.getResponse().getContentAsString().replaceAll("\"id\":\"[a-z]+_[a-z]+\"", "\"id\":\"<id>\""));
    }

    @Test
    void synchronous() throws Exception {
        var ok = post("/api/issues", "C-1001", """
                {"orderNumber":"NW-10442","message":"The flysheet arrived torn"}""");
        show("POST /api/issues", ok);
        assertThat(ok).hasStatus(200);

        var notYours = post("/api/issues", "C-1001", """
                {"orderNumber":"NW-10577","message":"The stove is broken"}""");
        show("POST /api/issues (someone else's order)", notYours);
        assertThat(notYours).hasStatus(404);
    }

    @Test
    void invalidInputIsRejectedBeforeTheAgentRuns() throws Exception {
        var invalid = post("/api/issues", "C-1001", """
                {"orderNumber":"10442","message":""}""");
        System.out.println("invalid input: " + invalid.getResponse().getStatus() + " "
                + invalid.getResponse().getContentType());
        assertThat(invalid).hasStatus(400);
    }

    @Test
    void asynchronousWithPollingAndOwnership() throws Exception {
        var started = post("/api/issues/async", "C-1001", """
                {"orderNumber":"NW-10442","message":"Wrong size sent"}""");
        String location = started.getResponse().getHeader("Location");
        show("POST /api/issues/async", started);
        System.out.println("Location: " + location.replaceAll("[a-z]+_[a-z]+$", "<id>"));
        assertThat(started).hasStatus(202);

        MvcTestResult outcome = null;
        for (int i = 0; i < 100; i++) {
            outcome = mvc.get().uri(location).header("X-Customer-Id", "C-1001").exchange();
            if (!outcome.getResponse().getContentAsString().contains("IN_PROGRESS")) {
                break;
            }
            Thread.sleep(50);
        }
        show("GET " + "/api/issues/<id>", outcome);

        var stranger = mvc.get().uri(location).header("X-Customer-Id", "C-2002").exchange();
        System.out.println("GET /api/issues/<id> as another customer: " + stranger.getResponse().getStatus());
        assertThat(stranger).hasStatus(404);
    }

    @Test
    void progressAsServerSentEvents() throws Exception {
        var started = post("/api/issues/async", "C-1001", """
                {"orderNumber":"NW-10442","message":"The flysheet arrived torn"}""");
        String location = started.getResponse().getHeader("Location");

        var events = mvc.get().uri(location + "/events").header("X-Customer-Id", "C-1001")
                .accept(MediaType.TEXT_EVENT_STREAM).exchange();
        System.out.println("GET /api/issues/<id>/events:\n" + events.getResponse().getContentAsString().strip());
        assertThat(events.getResponse().getContentAsString()).contains("data:Checking your order", "data:done");
    }
}
