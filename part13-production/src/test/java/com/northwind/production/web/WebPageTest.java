package com.northwind.production.web;

import io.opentelemetry.api.GlobalOpenTelemetry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 98: the page is served by the same application, and never inserts model output as HTML. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("lab")
class WebPageTest {

    @BeforeAll
    static void resetGlobalOpenTelemetry() {
        GlobalOpenTelemetry.resetForTest();
    }

    @Autowired MockMvcTester mvc;

    @Test
    void pageAndScriptAreServed() throws Exception {
        var root = mvc.get().uri("/").exchange();
        var page = mvc.get().uri("/index.html").exchange();
        var script = mvc.get().uri("/app.js").exchange().getResponse().getContentAsString();

        System.out.println("GET / forwards to: " + root.getResponse().getForwardedUrl());
        System.out.println("GET /index.html -> " + page.getResponse().getStatus() + " " + page.getResponse().getContentType()
                + ", form present: " + page.getResponse().getContentAsString().contains("id=\"issue-form\""));
        System.out.println("app.js uses innerHTML: " + script.contains("innerHTML")
                + ", uses textContent: " + script.contains("textContent"));
        assertThat(page).hasStatus(200);
        assertThat(script).doesNotContain("innerHTML");
    }
}
