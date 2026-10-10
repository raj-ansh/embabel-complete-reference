package com.northwind.tools.web;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.EarlyTerminationPolicy;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.domain.io.UserInput;
import com.northwind.tools.agent.BusinessAssistantAgent.AssistantAnswer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
public class AssistantController {

    private final AgentPlatform platform;

    public AssistantController(AgentPlatform platform) {
        this.platform = platform;
    }

    /** As in P3, the header stands in for the authenticated principal (Chapter 84). */
    @PostMapping("/api/assistant")
    public AssistantAnswer ask(@RequestHeader("X-Customer-Id") String customerId,
                               @Valid @RequestBody Question question) {
        ProcessOptions options = new ProcessOptions()
                .withToolCallContext(Map.of(
                        "customerId", customerId,                         // tools read it; the model never sees it
                        "roles", Set.of("customer"),                      // from the principal in a real app (Chapter 84)
                        "correlationId", UUID.randomUUID().toString()))
                .withAdditionalEarlyTerminationPolicy(EarlyTerminationPolicy.firstOf(
                        EarlyTerminationPolicy.maxActions(5),
                        EarlyTerminationPolicy.maxTokens(40_000),
                        EarlyTerminationPolicy.hardBudgetLimit(0.30)));
        return AgentInvocation.builder(platform).options(options).build(AssistantAnswer.class)
                .invoke(new UserInput(question.message()));
    }

    public record Question(@NotBlank @Size(max = 2000) String message) {
    }
}
