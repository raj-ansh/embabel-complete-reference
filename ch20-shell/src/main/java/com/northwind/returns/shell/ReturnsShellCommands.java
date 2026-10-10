package com.northwind.returns.shell;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.core.ProcessOptions;
import com.northwind.returns.domain.CustomerReply;
import com.northwind.returns.domain.ReturnRequest;
import com.northwind.returns.domain.ReturnRequest.ReturnReason;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

/**
 * A development-time command that skips natural-language routing and calls the returns
 * agent with a typed ReturnRequest, the same input the web form produces.
 */
@ShellComponent
public class ReturnsShellCommands {

    private final AgentPlatform agentPlatform;

    public ReturnsShellCommands(AgentPlatform agentPlatform) {
        this.agentPlatform = agentPlatform;
    }

    @ShellMethod(key = "return-form", value = "Run the returns agent with a structured return request")
    public String returnForm(
            @ShellOption(help = "Order number, e.g. NW-10442") String order,
            @ShellOption(help = "SKU of the item being returned") String sku,
            @ShellOption(help = "Reason: DAMAGED, WRONG_ITEM, NOT_AS_DESCRIBED, DOES_NOT_FIT, CHANGED_MIND",
                    defaultValue = "CHANGED_MIND") ReturnReason reason,
            @ShellOption(help = "Log the prompts sent to the model", defaultValue = "false") boolean showPrompts) {

        ProcessOptions defaults = new ProcessOptions();
        ProcessOptions options = defaults.withVerbosity(
                defaults.getVerbosity().withShowPrompts(showPrompts).withShowPlanning(true));

        CustomerReply reply = AgentInvocation.builder(agentPlatform)
                .options(options)
                .build(CustomerReply.class)
                .invoke(new ReturnRequest(order, sku, reason, null));

        return reply.subject() + System.lineSeparator() + System.lineSeparator() + reply.body();
    }
}
