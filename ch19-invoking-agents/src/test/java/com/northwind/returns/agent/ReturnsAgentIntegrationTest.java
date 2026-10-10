package com.northwind.returns.agent;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.ActionInvocation;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.northwind.returns.domain.CustomerReply;
import com.northwind.returns.domain.ReturnRequest;
import com.northwind.returns.domain.ReturnRequest.ReturnReason;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The real platform plans and runs the agent; only the two model calls are stubbed. */
class ReturnsAgentIntegrationTest extends EmbabelMockitoIntegrationTest {

    @Test
    void freeTextRunsAllFourActionsInTheOrderTheTypesImply() {
        whenCreateObject(p -> p.contains("<message>"), ReturnRequest.class)
                .thenReturn(new ReturnRequest("NW-10442", "TENT-2P", ReturnReason.DAMAGED, "pole snapped"));
        whenCreateObject(p -> p.contains("Decision: APPROVED"), CustomerReply.class)
                .thenReturn(new CustomerReply("Your return is approved", "Hi Asha, we're sorry…"));

        var process = AgentInvocation.create(agentPlatform, CustomerReply.class)
                .run(new UserInput("My tent pole snapped. Order NW-10442."));

        assertThat(process.getHistory()).extracting(ActionInvocation::getActionName)
                .map(name -> name.substring(name.lastIndexOf('.') + 1))
                .containsExactly("understand", "findOrder", "decide", "reply");
        assertThat(process.resultOfType(CustomerReply.class).subject()).contains("approved");
    }

    @Test
    void structuredInputSkipsTheLanguageStep() {
        whenCreateObject(p -> p.contains("Decision: REJECTED"), CustomerReply.class)
                .thenReturn(new CustomerReply("About your return", "Hi Mateo, unfortunately…"));

        var process = AgentInvocation.create(agentPlatform, CustomerReply.class)
                .run(new ReturnRequest("NW-09001", "BOOT-W9", ReturnReason.CHANGED_MIND, null));

        assertThat(process.getHistory()).extracting(ActionInvocation::getActionName)
                .noneMatch(name -> name.endsWith("understand"));
        verifyCreateObject(p -> p.contains("return window has passed"), CustomerReply.class);
    }
}
