package com.northwind.tenancy;

import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import com.northwind.tenancy.budget.TenantSpend;
import com.northwind.tenancy.lab.LabModels;
import com.northwind.tenancy.service.TenantSupportService;
import com.northwind.tenancy.service.TenantSupportService.Status;
import com.northwind.tenancy.tenant.TenantContext;
import com.northwind.tenancy.tenant.Tenants.Plan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Chapter 106: each tenant's plan chooses the model, its voice shapes the prompt, its budget caps the spend. */
@SpringBootTest
@ActiveProfiles("lab")
class TenantPlansTest {

    static final TenantContext ACME = new TenantContext("acme-outdoors", "Acme Outdoors", Plan.BASIC, "asha",
            "Be brief and friendly.");
    static final TenantContext PEAK = new TenantContext("peak-gear", "Peak Gear", Plan.PREMIUM, "chloe",
            "Be thorough and formal.");
    static final TenantContext TOTS = new TenantContext("trail-tots", "Trail Tots", Plan.BASIC, "tia", "Be cheerful.");

    @Autowired TenantSupportService support;
    @Autowired TenantSpend spend;
    @Autowired @Qualifier("labBasic") SpringAiLlmService basic;
    @Autowired @Qualifier("labPremium") SpringAiLlmService premium;

    @BeforeEach
    void clear() {
        LabModels.scripted(basic).clear();
        LabModels.scripted(premium).clear();
    }

    @Test
    void planChoosesTheModelAndVoiceShapesThePrompt() {
        support.answer(ACME, "What is your returns policy?");
        int acmeBasic = LabModels.scripted(basic).received().size();
        int acmePremium = LabModels.scripted(premium).received().size();
        String acmePrompt = LabModels.scripted(basic).received().getFirst().getContents();
        clear();
        support.answer(PEAK, "What is your returns policy?");
        int peakBasic = LabModels.scripted(basic).received().size();
        int peakPremium = LabModels.scripted(premium).received().size();
        String peakPrompt = LabModels.scripted(premium).received().getFirst().getContents();

        System.out.println("Acme Outdoors (BASIC):  lab-basic calls " + acmeBasic + ", lab-premium calls " + acmePremium
                + ", prompt says \"Be brief and friendly.\": " + acmePrompt.contains("Be brief and friendly."));
        System.out.println("Peak Gear (PREMIUM):    lab-basic calls " + peakBasic + ", lab-premium calls " + peakPremium
                + ", prompt says \"Be thorough and formal.\": " + peakPrompt.contains("Be thorough and formal."));
        assertThat(acmePremium).isZero();
        assertThat(peakBasic).isZero();
    }

    @Test
    void dailyBudgetStopsATenantButNotTheOthers() {
        var first = support.answer(TOTS, "What is your returns policy?");
        System.out.println("Trail Tots, first question:  " + first.status() + " (spent today: more than budget "
                + (spend.spentToday("trail-tots").doubleValue() > 0.00001) + ")");
        var second = support.answer(TOTS, "What is your returns policy?");
        System.out.println("Trail Tots, second question: " + second.status() + " - " + second.text());
        var other = support.answer(ACME, "What is your returns policy?");
        System.out.println("Acme Outdoors meanwhile:     " + other.status());
        assertThat(second.status()).isEqualTo(Status.OVER_BUDGET);
        assertThat(other.status()).isEqualTo(Status.ANSWERED);
    }
}
