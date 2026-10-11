package com.northwind.safety;

import com.northwind.safety.security.LabTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 84: each user has their own budget of requests per minute. */
@AutoConfigureMockMvc
@TestPropertySource(properties = "northwind.security.requests-per-minute=3")
class RateLimitTest extends LabTest {

    @Autowired MockMvcTester mvc;
    @Autowired LabTokens tokens;

    private int ask(String user) {
        return mvc.post().uri("/api/support/refund-request")
                .header("Authorization", "Bearer " + tokens.tokenFor(user, "northwind", "CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"message\": \"Where is my order NW-10442?\"}")
                .exchange().getResponse().getStatus();
    }

    @Test
    void fourthRequestInAMinuteIsRefused() {
        List<Integer> asha = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            asha.add(ask("C-1001"));
        }
        System.out.println("C-1001: " + asha);
        System.out.println("C-1002: " + ask("C-1002"));
        assertThat(asha).containsExactly(200, 200, 200, 429);
    }
}
