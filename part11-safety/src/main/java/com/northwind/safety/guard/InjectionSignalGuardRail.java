package com.northwind.safety.guard;

import com.embabel.agent.api.validation.guardrails.UserInputGuardRail;
import com.embabel.agent.core.Blackboard;
import com.embabel.common.core.validation.ValidationResult;
import com.embabel.common.core.validation.ValidationSeverity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Chapter 82: recognises common injection phrasing and reports it (WARNING), for monitoring. It does not
 * block: attackers rephrase easily, and legitimate customers sometimes write "ignore my last message".
 */
@Component
public class InjectionSignalGuardRail implements UserInputGuardRail {

    private static final List<Pattern> SIGNALS = List.of(
            Pattern.compile("(?i)ignore (all |any )?(previous|prior|above) instructions"),
            Pattern.compile("(?i)you are now\\b"),
            Pattern.compile("(?i)(reveal|repeat|print) (your|the) (system prompt|instructions)"));

    private final AtomicInteger signals = new AtomicInteger();

    @Override
    public String getName() {
        return "InjectionSignal";
    }

    @Override
    public String getDescription() {
        return "Reports text that looks like a prompt-injection attempt";
    }

    @Override
    public ValidationResult validate(String input, Blackboard blackboard) {
        if (SIGNALS.stream().anyMatch(p -> p.matcher(input).find())) {
            signals.incrementAndGet();
            return Verdicts.flag("INJECTION_SIGNAL", "Input resembles a prompt-injection attempt", ValidationSeverity.WARNING);
        }
        return Verdicts.pass();
    }

    public int signalsSeen() {
        return signals.get();
    }
}
