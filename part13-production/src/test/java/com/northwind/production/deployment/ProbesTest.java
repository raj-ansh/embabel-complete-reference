package com.northwind.production.deployment;

import io.opentelemetry.api.GlobalOpenTelemetry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 101: the probes the Kubernetes manifest uses exist in the prod profile and answer UP. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"lab", "prod"})
class ProbesTest {

    @BeforeAll
    static void resetGlobalOpenTelemetry() {
        GlobalOpenTelemetry.resetForTest();
    }

    @Autowired MockMvcTester mvc;

    @Test
    @SuppressWarnings("unchecked")
    void manifestProbesMatchTheApplication() throws Exception {
        Map<String, Object> deployment;
        try (var reader = new FileReader("k8s/deployment.yaml")) {
            deployment = new Yaml().load(reader);
        }
        var spec = (Map<String, Object>) ((Map<String, Object>) deployment.get("spec")).get("template");
        var container = ((List<Map<String, Object>>) ((Map<String, Object>) spec.get("spec")).get("containers")).getFirst();
        for (String probe : List.of("livenessProbe", "readinessProbe")) {
            var get = (Map<String, Object>) ((Map<String, Object>) container.get(probe)).get("httpGet");
            var result = mvc.get().uri((String) get.get("path")).exchange();
            System.out.println(probe + " " + get.get("path") + " (port " + get.get("port") + ") -> "
                    + result.getResponse().getStatus() + " " + result.getResponse().getContentAsString());
            assertThat(result).hasStatus(200);
        }
    }
}
