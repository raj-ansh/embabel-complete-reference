package com.northwind.tools;

import com.embabel.agent.api.tool.CommunicateTool;
import com.embabel.agent.api.tool.ProgressTool;
import com.embabel.agent.api.tool.Tool;
import com.embabel.agent.api.tool.progressive.UnfoldingTool;
import com.embabel.agent.tools.blackboard.BlackboardTools;
import com.embabel.agent.tools.process.AgentProcessTools;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 45: the built-in introspection and communication tools, inspected and called without a model. */
class IntrospectionToolsTest {

    private static java.util.List<String> names(UnfoldingTool tool) {
        return tool.getInnerTools().stream().map(t -> t.getDefinition().getName()).toList();
    }

    @Test
    void introspectionToolsAreUnfoldingFacades() {
        UnfoldingTool process = new AgentProcessTools().create();
        UnfoldingTool blackboard = new BlackboardTools().create();

        System.out.println(process.getDefinition().getName() + " -> " + names(process));
        System.out.println(blackboard.getDefinition().getName() + " -> " + names(blackboard));

        assertThat(names(process)).contains("process_status", "process_budget", "process_cost",
                "process_history", "process_tools_stats", "process_models");
        assertThat(names(blackboard)).contains("blackboard_list", "blackboard_get", "blackboard_last",
                "blackboard_describe", "blackboard_count");
    }

    @Test
    void communicationToolsDegradeGracefullyOutsideAProcess() {
        Tool progress = ProgressTool.INSTANCE.create();
        Tool communicate = CommunicateTool.INSTANCE.create();

        System.out.println(progress.getDefinition().getName() + " schema: " + progress.getDefinition().getInputSchema().toJsonSchema());
        System.out.println(communicate.getDefinition().getName() + " schema: " + communicate.getDefinition().getInputSchema().toJsonSchema());

        Tool.Result result = communicate.call("{\"message\": \"Your report is ready.\"}");
        System.out.println("communicate outside a process: " + result);

        assertThat(progress.getDefinition().getName()).isEqualTo(ProgressTool.NAME);
        assertThat(communicate.getDefinition().getName()).isEqualTo(CommunicateTool.NAME);
        assertThat(result).isNotNull();   // soft acknowledgement, no exception
    }
}
