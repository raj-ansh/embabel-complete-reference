package com.northwind.agents.config;

import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Starts the application with the "prod" profile and fake model names (as a deployment would
 * supply them) and checks that every layer of configuration resolves as intended.
 */
@ActiveProfiles("prod")
@TestPropertySource(properties = {
        "NORTHWIND_LLM_CHEAP=test-small",
        "NORTHWIND_LLM_BALANCED=test-medium",
        "NORTHWIND_LLM_BEST=test-large",
        "NORTHWIND_RETURNS_AUTO_APPROVE_LIMIT=150.00"
})
class ConfigurationBindingTest extends EmbabelMockitoIntegrationTest {

    @Autowired
    NorthwindAgentProperties properties;

    @Autowired
    Environment environment;

    @Test
    void ourPropertiesBindAndProdOverridesApply() {
        assertThat(properties.supportEmail()).isEqualTo("support@northwind.example");
        assertThat(properties.replyMaxWords()).isEqualTo(120);
        assertThat(properties.returns().autoApproveLimit()).isEqualByComparingTo("150.00");   // env wins over prod default
    }

    @Test
    void modelRolesResolveFromTheEnvironment() {
        // EmbabelMockitoIntegrationTest itself sets embabel.models.default-llm=test-model for tests
        assertThat(environment.getProperty("embabel.models.default-llm")).isEqualTo("test-model");
        assertThat(environment.getProperty("embabel.models.llms.cheap")).isEqualTo("test-small");
        assertThat(environment.getProperty("embabel.models.llms.best")).isEqualTo("test-large");
    }

    @Test
    void builtInProcessEndpointsAreSwitchedOff() {
        assertThat(environment.getProperty("embabel.agent.platform.rest.process-kill-enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("embabel.agent.platform.toolloop.max-iterations", Integer.class)).isEqualTo(10);
    }
}
