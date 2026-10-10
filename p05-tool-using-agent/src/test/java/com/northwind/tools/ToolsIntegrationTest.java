package com.northwind.tools;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.api.tool.Tool;
import com.embabel.agent.core.ProcessOptions;
import com.embabel.agent.core.ToolGroupMetadata;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.northwind.tools.advanced.AdvancedTools;
import com.northwind.tools.agent.BusinessAssistantAgent.AssistantAnswer;
import com.northwind.tools.data.ProductCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ToolsIntegrationTest extends EmbabelMockitoIntegrationTest {

    @Autowired
    ProductCatalogService catalog;

    @Autowired
    AdvancedTools advanced;

    @Test
    void databaseToolGoesThroughTheSafeFacade() {
        var tents = catalog.search("tent");

        assertThat(tents).extracting(ProductCatalogService.ProductView::sku).contains("TENT-2P", "TENT-4P");
        assertThat(tents.getFirst().toString()).doesNotContain("unitCost", "71.20");   // internal cost never exposed
        assertThat(catalog.search("x")).isEmpty();                                     // too short: no query
    }

    @Test
    void platformToolGroupsAreResolvedByRole() {
        List<String> roles = agentPlatform.getToolGroupResolver().availableToolGroups().stream()
                .map(ToolGroupMetadata::getRole).toList();

        System.out.println("Tool group roles: " + roles);
        assertThat(roles).contains("math");
    }

    @Test
    void theAgentOffersExactlyTheChosenToolsToTheModel() {
        whenCreateObject(p -> p.contains("<question>"), AssistantAnswer.class,
                interaction -> {
                    List<String> tools = interaction.getTools().stream().map(t -> t.getDefinition().getName()).toList();
                    System.out.println("Tools offered: " + tools);
                    return Set.copyOf(tools).equals(Set.of(                // exactly these: a new tool must be added here
                            "lineTotal", "applyDiscount", "forecast", "currentCustomer", "orderStatus", "myOrders",
                            "searchProducts", "inStockByCategory", "trackParcel", "draftEmailToCustomer",
                            "searchPolicies", "readPolicy", "evaluatePromotions", "holdForPickup"))
                            && interaction.getToolGroups().stream().anyMatch(g -> g.getRole().equals("math"));
                })
                .thenReturn(new AssistantAnswer("Your order NW-10442 has shipped.", List.of("orderStatus")));

        AssistantAnswer answer = AgentInvocation.builder(agentPlatform)
                .options(new ProcessOptions().withToolCallContext(Map.of("customerId", "C-1001")))
                .build(AssistantAnswer.class)
                .invoke(new UserInput("Where is NW-10442?"));

        assertThat(answer.answer()).contains("shipped");
    }

    @Test
    void advancedToolsHaveTheExpectedShape() {
        assertThat(advanced.shippingEstimateTool().getDefinition().getName()).isEqualTo("estimate_shipping");
        assertThat(advanced.shippingEstimateTool().getDefinition().getInputSchema().toJsonSchema()).contains("zone", "weightKg");
        assertThat(advanced.basketAdvisor().getDefinition().getName()).isEqualTo("basket_advisor");
        assertThat(advanced.policyResearcher().getDefinition().getName()).isEqualTo("policy_researcher");
        assertThat(advanced.storeOperations().getDefinition().getDescription()).contains("Invoke to see the tools");
        assertThat(advanced.storeOperations().getInnerTools()).extracting(t -> t.getDefinition().getName())
                .contains("searchProducts", "inStockByCategory", "trackParcel");
        assertThat(advanced.selfAwareResearcher().getTools()).extracting(t -> t.getDefinition().getName())
                .contains("agent_process", "blackboard", "progress", "communicate", "searchProducts");
    }

    @Test
    void typedToolRunsWithoutAModel() {
        Tool.Result result = advanced.shippingEstimateTool().call("""
                {"zone": "Z1", "weightKg": 2.5}""");
        System.out.println("estimate_shipping -> " + result);
        assertThat(result.toString()).contains("Z1");
    }
}
