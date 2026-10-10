package com.northwind.docs.web;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.EarlyTerminationPolicy;
import com.embabel.agent.core.ProcessOptions;
import com.northwind.docs.agent.Docs.Asker;
import com.northwind.docs.agent.Docs.Audience;
import com.northwind.docs.agent.Docs.DocQuestion;
import com.northwind.docs.agent.Docs.GroundedAnswer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/** POST /api/docs/ask. Headers stand in for authentication (Chapter 84). */
@RestController
public class DocsController {

    public record Ask(@NotBlank @Size(max = 1000) String question) {
    }

    private final AgentPlatform platform;

    public DocsController(AgentPlatform platform) {
        this.platform = platform;
    }

    @PostMapping("/api/docs/ask")
    public GroundedAnswer ask(@RequestHeader("X-User-Id") String userId,
                              @RequestHeader(value = "X-Roles", defaultValue = "") String roles,
                              @Valid @RequestBody Ask body) {
        Audience audience = Arrays.asList(roles.split(",")).contains("staff") ? Audience.STAFF : Audience.CUSTOMER;
        return AgentInvocation.builder(platform)
                .options(new ProcessOptions().withAdditionalEarlyTerminationPolicy(EarlyTerminationPolicy.firstOf(
                        EarlyTerminationPolicy.maxActions(3), EarlyTerminationPolicy.hardBudgetLimit(0.10))))
                .build(GroundedAnswer.class)
                .invoke(new DocQuestion(body.question()), new Asker(userId, audience));
    }
}
