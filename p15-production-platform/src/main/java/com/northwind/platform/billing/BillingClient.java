package com.northwind.platform.billing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/** P15: NorthWind's billing system is another service, reached over HTTP. Its invoices name their tenant. */
@Component
public class BillingClient {

    public record Invoice(String number, String tenant, String customerId, BigDecimal amount, String status) {
    }

    public record RefundReceipt(String reference) {
    }

    private final RestClient http;

    public BillingClient(RestClient.Builder builder, @Value("${northwind.billing.url}") String baseUrl) {
        this.http = builder.baseUrl(baseUrl).build();
    }

    public Optional<Invoice> invoice(String number) {
        try {
            return Optional.ofNullable(http.get().uri("/invoices/{number}", number).retrieve().body(Invoice.class));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    public RefundReceipt refund(String invoice, BigDecimal amount) {
        return http.post().uri("/refunds")
                .body(Map.of("invoice", invoice, "amount", amount))
                .retrieve().body(RefundReceipt.class);
    }
}
