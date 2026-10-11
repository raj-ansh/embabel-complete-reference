package com.northwind.safety.privacy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chapter 83: replaces personal data with placeholders before text goes to a model, and puts it back
 * afterwards. The model works with "[EMAIL_1]"; the customer sees their real address. One instance per
 * conversation: the mapping must never be shared between customers.
 */
public final class Pseudonymiser {

    private record Kind(String label, Pattern pattern) {
    }

    private static final List<Kind> KINDS = List.of(
            new Kind("EMAIL", Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+")),
            new Kind("PHONE", Pattern.compile("(?:\\+44\\s?7\\d{3}|\\b07\\d{3})\\s?\\d{3}\\s?\\d{3}\\b")),
            new Kind("POSTCODE", Pattern.compile("\\b[A-Z]{1,2}\\d[A-Z\\d]?\\s?\\d[A-Z]{2}\\b")));

    private final Map<String, String> placeholders = new LinkedHashMap<>();   // value -> placeholder

    public String mask(String text) {
        String masked = text;
        for (Kind kind : KINDS) {
            Matcher m = kind.pattern().matcher(masked);
            StringBuilder out = new StringBuilder();
            while (m.find()) {
                String placeholder = placeholders.computeIfAbsent(m.group(),
                        value -> "[" + kind.label() + "_" + (count(kind.label()) + 1) + "]");
                m.appendReplacement(out, Matcher.quoteReplacement(placeholder));
            }
            m.appendTail(out);
            masked = out.toString();
        }
        return masked;
    }

    public String restore(String text) {
        String restored = text;
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            restored = restored.replace(e.getValue(), e.getKey());
        }
        return restored;
    }

    public int masked() {
        return placeholders.size();
    }

    private long count(String label) {
        return placeholders.values().stream().filter(p -> p.startsWith("[" + label + "_")).count();
    }
}
