package com.northwind.enterprise;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 109: P14 over HTTP. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("lab")
class ApiTest {

    @DynamicPropertySource
    static void billing(DynamicPropertyRegistry registry) throws IOException {
        String url = BillingStub.start();
        registry.add("northwind.billing.url", () -> url);
    }

    @Autowired MockMvcTester mvc;

    @Test
    void askOverHttp() throws Exception {
        var result = mvc.post().uri("/api/requests").header("X-Customer-Id", "C-1001")
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"Where is my order NW-40001?\"}").exchange();
        System.out.println("POST /api/requests: " + result.getResponse().getStatus() + " "
                + result.getResponse().getContentAsString());
        assertThat(result).hasStatus(200);
        assertThat(result.getResponse().getContentAsString()).contains("\"route\":\"SUPPORT\"");
    }
}
