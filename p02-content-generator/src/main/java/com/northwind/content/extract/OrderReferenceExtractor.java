package com.northwind.content.extract;

import com.embabel.agent.api.common.Ai;
import com.embabel.common.ai.model.LlmOptions;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds an order number in free text. Code first (a regex is free and exact);
 * the model only for messages the regex cannot handle, and "not found" is a normal outcome.
 */
@Service
public class OrderReferenceExtractor {

    private static final Pattern ORDER = Pattern.compile("\\bNW-\\d{4,6}\\b", Pattern.CASE_INSENSITIVE);

    private final Ai ai;

    public OrderReferenceExtractor(Ai ai) {
        this.ai = ai;
    }

    public Optional<OrderReference> extract(String message) {
        Matcher m = ORDER.matcher(message);
        if (m.find()) {
            return Optional.of(new OrderReference(m.group().toUpperCase()));   // no model call needed
        }
        OrderReference fromModel = ai.withLlm(LlmOptions.withLlmForRole("cheap").withTemperature(0.0))
                .createObjectIfPossible("""
                        If the message below refers to a NorthWind order number (format NW- followed by
                        4 to 6 digits, possibly written with spaces or words), return it normalised.
                        If there is no order number, do not invent one.

                        <message>
                        %s
                        </message>""".formatted(message), OrderReference.class);
        return Optional.ofNullable(fromModel);
    }
}
