package com.northwind.platform.tools;

import com.embabel.agent.api.annotation.LlmTool;
import com.embabel.agent.api.tool.ToolCallContext;
import com.northwind.platform.billing.BillingClient;
import com.northwind.platform.billing.RefundDesk;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** P15: billing tools for the model. Identity from tool call context; decisions in RefundDesk. */
@Component
public class BillingTools {

    private final BillingClient billing;
    private final RefundDesk refunds;

    public BillingTools(BillingClient billing, RefundDesk refunds) {
        this.billing = billing;
        this.refunds = refunds;
    }

    @LlmTool(description = "Amount and payment status of one of the signed-in customer's invoices")
    public String invoiceStatus(@LlmTool.Param(description = "Invoice number, e.g. INV-7001") String invoiceNumber,
                                ToolCallContext context) {
        return billing.invoice(invoiceNumber)
                .filter(invoice -> invoice.tenant().equals(context.get("tenant"))
                        && invoice.customerId().equals(context.get("customerId")))
                .map(invoice -> invoice.number() + ": " + invoice.amount() + " GBP, " + invoice.status())
                .orElse("No invoice " + invoiceNumber + " was found on your account.");
    }

    @LlmTool(description = "Ask for a refund on one of the signed-in customer's invoices")
    public String requestRefund(@LlmTool.Param(description = "Invoice number") String invoiceNumber,
                                @LlmTool.Param(description = "Amount in GBP") BigDecimal amount,
                                @LlmTool.Param(description = "The customer's reason, in a few words") String reason,
                                ToolCallContext context) {
        return refunds.request((String) context.get("tenant"), (String) context.get("customerId"),
                invoiceNumber, amount, reason);
    }
}
