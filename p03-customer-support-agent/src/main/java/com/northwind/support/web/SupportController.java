package com.northwind.support.web;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.EarlyTerminationPolicy;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.domain.io.UserInput;
import com.northwind.support.domain.SupportDomain.CustomerContext;
import com.northwind.support.domain.SupportDomain.SupportResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SupportController {

    private final AgentPlatform platform;

    public SupportController(AgentPlatform platform) {
        this.platform = platform;
    }

    /**
     * In this project the customer ID arrives in a header to keep the example small.
     * In production it comes from the authenticated principal (Chapter 84), never from the client.
     */
    @PostMapping("/api/support")
    public SupportResponse ask(@RequestHeader("X-Customer-Id") String customerId,
                               @Valid @RequestBody Question question) {
        return AgentInvocation.builder(platform)
                .options(new ProcessOptions().withAdditionalEarlyTerminationPolicy(EarlyTerminationPolicy.firstOf(
                        EarlyTerminationPolicy.maxActions(8),
                        EarlyTerminationPolicy.hardBudgetLimit(0.20),
                        EarlyTerminationPolicy.maxTokens(15_000))))
                .build(SupportResponse.class)
                .invoke(new UserInput(question.message()), new CustomerContext(customerId));
    }

    public record Question(@NotBlank @Size(max = 2000) String message) {
    }
}
