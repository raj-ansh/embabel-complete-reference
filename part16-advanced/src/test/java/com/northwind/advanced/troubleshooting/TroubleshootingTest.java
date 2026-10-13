package com.northwind.advanced.troubleshooting;

import com.embabel.agent.api.common.Ai;
import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import com.northwind.advanced.lab.LabModels;
import com.northwind.advanced.troubleshooting.RefundCaseAgent.CustomerRecord;
import com.northwind.advanced.troubleshooting.RefundCaseAgent.RefundCase;
import com.northwind.advanced.troubleshooting.RefundCaseAgent.RefundDecision;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 118: common failures, reproduced, with the messages they produce. */
@SpringBootTest
@ActiveProfiles("lab")
class TroubleshootingTest {

    /** A type no agent produces. */
    public record LoyaltyReport(String customerId) {
    }

    @Autowired AgentPlatform platform;
    @Autowired Ai ai;
    @Autowired OrderLookupTools orderTools;
    @Autowired @Qualifier("labStandard") SpringAiLlmService standard;

    private static String describe(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String message = String.valueOf(root.getMessage()).replaceAll("\\s+", " ");
        return root.getClass().getSimpleName() + ": " + (message.length() > 150 ? message.substring(0, 150) + "..." : message);
    }

    @Test
    void stuckWhenAnInputCanNeverExist() {
        var stuck = AgentInvocation.create(platform, RefundDecision.class).run(new RefundCase("NW-40001", 120));
        System.out.println("with a RefundCase only: status " + stuck.getStatus() + ", actions run " + stuck.getHistory().size());
        var done = AgentInvocation.create(platform, RefundDecision.class)
                .run(new RefundCase("NW-40001", 120), new CustomerRecord("C-1001", 7));
        System.out.println("with a CustomerRecord too: status " + done.getStatus() + ", result " + done.last(RefundDecision.class));
        assertThat(stuck.getStatus().name()).isEqualTo("STUCK");
        assertThat(done.getStatus().name()).isEqualTo("COMPLETED");
    }

    @Test
    void noAgentProducesTheType() {
        try {
            AgentInvocation.create(platform, LoyaltyReport.class).invoke(new RefundCase("NW-40001", 10));
            System.out.println("no agent for LoyaltyReport: no exception");
        } catch (Exception e) {
            System.out.println("no agent for LoyaltyReport: " + describe(e));
        }
    }

    @Test
    void unknownModelRole() {
        try {
            ai.withLlmByRole("premium").generateText("Hello");
            System.out.println("unknown role: no exception");
        } catch (Exception e) {
            System.out.println("unknown role: " + describe(e));
        }
    }

    @Test
    void outputThatIsNotJson() {
        var model = LabModels.scripted(standard);
        model.clear();
        try {
            ai.withLlmByRole("standard").createObject("PARSE-ME: decide this refund", RefundDecision.class);
            System.out.println("not JSON: no exception");
        } catch (Exception e) {
            System.out.println("not JSON: " + describe(e));
        }
        System.out.println("model calls before giving up: " + model.received().size());
    }

    @Test
    void aToolThatFails() {
        String answer = ai.withLlmByRole("standard").withToolObject(orderTools).generateText("Where is order NW-40001?");
        System.out.println("what the model saw after the tool failed: " + answer);
    }
}
