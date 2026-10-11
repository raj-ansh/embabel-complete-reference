package com.northwind.safety.injection;

import com.embabel.agent.api.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Chapter 82: tools from outside the team are vetted before a model sees them. Only allowlisted names
 * are offered, and a description that tells the model what to do (rather than what the tool does) is
 * rejected even for an allowlisted tool, because descriptions can change without a release.
 */
@Component
public class ToolVetting {

    public record Rejected(String tool, String reason) {
    }

    public record Vetted(List<Tool> offered, List<Rejected> rejected) {
    }

    private static final Set<String> ALLOWED = Set.of("partnerSearch", "productReviews", "issueRefund");
    private static final Pattern INSTRUCTION = Pattern.compile("(?i)\\b(always|first|must|ignore|instead)\\b.*\\b(call|use|run)\\b");

    public Vetted vet(List<Tool> candidates) {
        List<Tool> offered = new ArrayList<>();
        List<Rejected> rejected = new ArrayList<>();
        for (Tool tool : candidates) {
            String name = tool.getDefinition().getName();
            String description = tool.getDefinition().getDescription();
            if (!ALLOWED.contains(name)) {
                rejected.add(new Rejected(name, "not on the allowlist"));
            } else if (INSTRUCTION.matcher(description).find()) {
                rejected.add(new Rejected(name, "description contains instructions: \"" + description + "\""));
            } else {
                offered.add(tool);
            }
        }
        return new Vetted(offered, rejected);
    }
}
