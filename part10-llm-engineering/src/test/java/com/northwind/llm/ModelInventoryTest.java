package com.northwind.llm;

import com.embabel.agent.spi.LlmService;
import com.embabel.common.ai.model.ModelProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 72: what the platform registered at startup, and what each role means. */
class ModelInventoryTest extends LabTest {

    @Autowired ModelProvider modelProvider;

    @Test
    void inventory() {
        modelProvider.listModels().forEach(m -> System.out.println("model: " + m.getName() + " (" + m.getProvider() + ")"));
        System.out.println("roles: " + modelProvider.listRoles(LlmService.class));
        for (String role : modelProvider.listRoles(LlmService.class)) {
            System.out.println("  " + role + " -> " + modelProvider.configuredOptionsForRole(role).getModelName());
        }
        assertThat(modelProvider.listModelNames(LlmService.class)).contains("lab-mini", "lab-standard", "lab-reasoner");
    }
}
