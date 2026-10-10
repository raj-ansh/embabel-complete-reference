package com.northwind.returns.shell;

import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.northwind.returns.domain.CustomerReply;
import com.northwind.returns.domain.ReturnRequest.ReturnReason;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/** Shell commands are ordinary beans: test them by calling the method. The interactive shell is switched off for tests. */
@TestPropertySource(properties = "embabel.agent.shell.interactive.enabled=false")   // see the note in Chapter 20
class ReturnsShellCommandsTest extends EmbabelMockitoIntegrationTest {

    @Autowired
    ReturnsShellCommands commands;

    @Test
    void returnFormRunsTheAgentWithTypedInput() {
        whenCreateObject(p -> p.contains("Decision: APPROVED"), CustomerReply.class)
                .thenReturn(new CustomerReply("Your return is approved", "Hi Asha…"));

        String output = commands.returnForm("NW-10442", "TENT-2P", ReturnReason.DAMAGED, false);

        assertThat(output).startsWith("Your return is approved").contains("Hi Asha");
    }
}
