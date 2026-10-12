package com.northwind.production.integration;

import com.northwind.production.domain.Issues.IssueReport;
import com.northwind.production.domain.Issues.Remedy;
import com.northwind.production.service.IssueService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ActiveProfilesResolver;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chapter 91: an evaluation suite. It runs real customer messages through the whole agent and scores the
 * outcomes, instead of asserting exact model output. By default it uses the offline lab models; run it
 * against a real model with -Dnorthwind.eval.profile=default and a provider profile (for example -Popenai).
 */
@SpringBootTest
@ActiveProfiles(resolver = IssueEvaluationTest.EvaluationProfile.class)
@Tag("evaluation")
class IssueEvaluationTest {

    /** Accuracy below this fails the build: a prompt or model change made things worse. */
    static final double REQUIRED_ACCURACY = 0.80;

    public static class EvaluationProfile implements ActiveProfilesResolver {
        @Override
        public String[] resolve(Class<?> testClass) {
            return new String[] {System.getProperty("northwind.eval.profile", "lab")};
        }
    }

    record Case(String order, String message, Remedy expected) {
    }

    @Autowired IssueService issues;

    @Test
    void remediesMatchWhatAPersonWouldDecide() throws Exception {
        List<Case> cases = new ArrayList<>();
        for (String line : new ClassPathResource("evaluation/issues.csv").getContentAsString(StandardCharsets.UTF_8).split("\n")) {
            if (!line.isBlank() && !line.startsWith("#")) {
                String[] f = line.strip().split("\\|");
                cases.add(new Case(f[0], f[1], Remedy.valueOf(f[2])));
            }
        }
        int correct = 0;
        for (Case c : cases) {
            var outcome = issues.resolve(new IssueReport("C-1001", c.order(), c.message()));
            Remedy actual = outcome.reply() == null ? null : outcome.reply().remedy();
            boolean ok = actual == c.expected();
            correct += ok ? 1 : 0;
            if (!ok) {
                System.out.println("MISS  expected " + c.expected() + ", got " + actual + ": " + c.message());
            }
        }
        double accuracy = (double) correct / cases.size();
        System.out.printf("evaluation: %d/%d correct (%.0f%%), required %.0f%%%n",
                correct, cases.size(), accuracy * 100, REQUIRED_ACCURACY * 100);
        assertThat(accuracy).isGreaterThanOrEqualTo(REQUIRED_ACCURACY);
    }
}
