package com.northwind.platform.billing;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * P15: refunds, decided in code. Small refunds on the customer's own invoices are paid at once; larger ones
 * wait in a table for a person to approve. The model can ask for a refund; it can never grant one.
 */
@Service
public class RefundDesk {

    public static final BigDecimal AUTO_APPROVE_LIMIT = new BigDecimal("50.00");

    public record PendingRefund(long id, String tenant, String customerId, String invoice, BigDecimal amount, String reason) {
    }

    private final JdbcClient jdbc;
    private final BillingClient billing;

    public RefundDesk(JdbcClient jdbc, BillingClient billing) {
        this.jdbc = jdbc;
        this.billing = billing;
    }

    public String request(String tenant, String customerId, String invoiceNumber, BigDecimal amount, String reason) {
        var invoice = billing.invoice(invoiceNumber)
                .filter(i -> i.tenant().equals(tenant) && i.customerId().equals(customerId));
        if (invoice.isEmpty()) {
            return "No invoice " + invoiceNumber + " was found on your account.";
        }
        if (amount.signum() <= 0 || amount.compareTo(invoice.get().amount()) > 0) {
            return "A refund must be more than zero and no more than the invoice total of "
                    + invoice.get().amount() + " GBP.";
        }
        if (amount.compareTo(AUTO_APPROVE_LIMIT) <= 0) {
            return "Refunded " + amount + " GBP (reference " + billing.refund(invoiceNumber, amount).reference() + ").";
        }
        var key = new GeneratedKeyHolder();
        jdbc.sql("""
                        INSERT INTO refund_request (tenant, customer_id, invoice, amount, reason, status)
                        VALUES (:tenant, :customer, :invoice, :amount, :reason, 'PENDING')""")
                .param("tenant", tenant).param("customer", customerId).param("invoice", invoiceNumber)
                .param("amount", amount).param("reason", reason)
                .update(key, "id");
        return "Your refund of " + amount + " GBP has been sent to our team for approval (request "
                + key.getKey().longValue() + "). You will hear from us within one working day.";
    }

    /** Staff see only their own tenant's queue (Chapter 107). */
    public List<PendingRefund> pending(String tenant) {
        return jdbc.sql("SELECT id, tenant, customer_id, invoice, amount, reason FROM refund_request "
                        + "WHERE status = 'PENDING' AND tenant = :tenant")
                .param("tenant", tenant).query(PendingRefund.class).list();
    }

    public String approve(long id, String staffMember, String tenant) {
        var refund = jdbc.sql("SELECT id, tenant, customer_id, invoice, amount, reason FROM refund_request "
                        + "WHERE id = :id AND status = 'PENDING' AND tenant = :tenant")
                .param("id", id).param("tenant", tenant).query(PendingRefund.class).optional()
                .orElseThrow(() -> new IllegalArgumentException("No pending refund " + id));
        String reference = billing.refund(refund.invoice(), refund.amount()).reference();
        jdbc.sql("UPDATE refund_request SET status = 'APPROVED', decided_by = :staff WHERE id = :id")
                .param("staff", staffMember).param("id", id).update();
        return reference;
    }
}
