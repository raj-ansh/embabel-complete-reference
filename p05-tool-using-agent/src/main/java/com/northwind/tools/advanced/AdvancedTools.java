package com.northwind.tools.advanced;

import com.embabel.agent.api.tool.CommunicateTool;
import com.embabel.agent.api.tool.ProgressTool;
import com.embabel.agent.api.tool.Tool;
import com.embabel.agent.api.tool.agentic.playbook.PlaybookTool;
import com.embabel.agent.api.tool.agentic.simple.SimpleAgenticTool;
import com.embabel.agent.api.tool.progressive.UnfoldingTool;
import com.embabel.agent.tools.blackboard.BlackboardTools;
import com.embabel.agent.tools.process.AgentProcessTools;
import com.embabel.common.ai.model.LlmOptions;
import com.northwind.tools.catalog.CalculatorTools;
import com.northwind.tools.data.ProductTools;
import com.northwind.tools.docs.FileSearchTools;
import com.northwind.tools.integration.TrackingTools;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Chapters 41, 43, 44 and 45: tools built from tools.
 * - a framework-agnostic, strongly typed tool, decorated to publish its result to the blackboard
 * - an agentic tool: a small LLM-orchestrated workflow exposed as ONE tool
 * - a playbook tool whose second step unlocks only after the first
 * - an unfolding (progressive) tool that hides detail until the model asks for it
 */
@Component
public class AdvancedTools {

    public record ShippingEstimateRequest(String zone, double weightKg) {
    }

    public record ShippingEstimate(String zone, double price, int days) {
    }

    private final CalculatorTools calculator;
    private final ProductTools products;
    private final TrackingTools tracking;
    private final FileSearchTools policies;

    public AdvancedTools(CalculatorTools calculator, ProductTools products,
                         TrackingTools tracking, FileSearchTools policies) {
        this.calculator = calculator;
        this.products = products;
        this.tracking = tracking;
        this.policies = policies;
    }

    /** Chapter 41: typed, framework-agnostic tool; its result is also published to the blackboard. */
    public Tool shippingEstimateTool() {
        Tool estimate = Tool.fromFunction(
                "estimate_shipping",
                "Estimate shipping price and delivery days for a parcel to a zone",
                ShippingEstimateRequest.class,
                ShippingEstimate.class,
                req -> new ShippingEstimate(req.zone(), 6.5 + 1.2 * req.weightKg(), "Z1".equals(req.zone()) ? 2 : 4));
        return Tool.publishToBlackboard(estimate);
    }

    /** Chapter 43: an agentic tool. The outer model calls one tool; an inner model orchestrates three. */
    public SimpleAgenticTool basketAdvisor() {
        return new SimpleAgenticTool("basket_advisor",
                "Work out the cheapest in-stock option for a list of product needs, with exact totals")
                .withToolObject(products)
                .withToolObject(calculator)
                .withLlm(LlmOptions.withLlmForRole("cheap"))
                .withMaxIterations(6);
    }

    /** Chapter 43: a playbook. Reading a policy unlocks only after searching for it. */
    public PlaybookTool policyResearcher() {
        List<Tool> policyTools = Tool.fromInstance(policies);
        Tool search = policyTools.stream().filter(t -> t.getDefinition().getName().equals("searchPolicies")).findFirst().orElseThrow();
        Tool read = policyTools.stream().filter(t -> t.getDefinition().getName().equals("readPolicy")).findFirst().orElseThrow();
        return new PlaybookTool("policy_researcher", "Find and read the NorthWind policy that answers a question")
                .withTools(search)
                .withTool(read).unlockedBy(search);
    }

    /** Chapter 44: progressive disclosure. One short description up front; details on demand. */
    public UnfoldingTool storeOperations() {
        return UnfoldingTool.of(
                "store_operations",
                "Store operations: catalog search, stock by category and parcel tracking. Invoke to see the tools.",
                Tool.fromInstance(products))
                .withToolObject(tracking);
    }

    /**
     * Chapter 45: an assistant that can see its own process (budget, cost, history), read earlier results
     * from the blackboard, and talk to the user while it works. In Java the two communication tools are
     * Kotlin objects, so we go through INSTANCE.
     */
    public SimpleAgenticTool selfAwareResearcher() {
        return new SimpleAgenticTool("self_aware_researcher",
                "Research a product question, checking remaining budget first and reporting progress")
                .withTools(
                        new AgentProcessTools().create(),
                        new BlackboardTools().create(),
                        ProgressTool.INSTANCE.create(),
                        CommunicateTool.INSTANCE.create())
                .withToolObject(products)
                .withSystemPrompt("""
                        Before any search, call process_budget. If less than 20% of the cost budget remains,
                        answer from what is already on the blackboard. Use progress for short status updates
                        and communicate only for the final answer.""")
                .withMaxIterations(8);
    }
}
