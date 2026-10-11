package com.northwind.mcpserver.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Export;
import com.embabel.agent.mcpserver.security.SecureAgentTool;
import com.embabel.agent.api.common.OperationContext;
import com.northwind.mcpserver.domain.Catalog.ProductAdvice;
import com.northwind.mcpserver.domain.Catalog.TripQuestion;
import com.northwind.mcpserver.tools.CatalogTools;

/**
 * P11: a goal that uses a model and tools internally. The MCP client sees one tool; it neither knows nor
 * cares that an agent with its own model, prompt and tools answers it.
 */
@Agent(description = "Recommend NorthWind products for an outdoor trip")
@SecureAgentTool("hasAuthority('products:read')")            // Chapter 88
public class ProductAdvisorAgent {

    @AchievesGoal(description = "Recommend in-stock NorthWind products for an outdoor trip described in plain English",
            export = @Export(remote = true, name = "productAdvice", startingInputTypes = {TripQuestion.class}))
    @Action(description = "Recommend products")
    public ProductAdvice advise(TripQuestion question, OperationContext context) {
        return context.ai()
                .withLlmByRole("balanced")
                .withToolObject(new CatalogTools())
                .createObject("""
                        Recommend NorthWind products for the trip between the <trip> tags. Check stock first.
                        <trip>
                        %s
                        </trip>""".formatted(question.trip()), ProductAdvice.class);
    }
}
