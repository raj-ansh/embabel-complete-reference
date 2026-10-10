package com.northwind.tools.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

/**
 * A REST client for the parcel carrier's tracking API. The base URL is fixed by configuration:
 * the model can never choose which host is called (no SSRF).
 */
@Component
public class CarrierClient {

    @ConfigurationProperties(prefix = "northwind.carrier")
    public record CarrierProperties(String baseUrl, Duration timeout) {
    }

    public record TrackingInfo(String trackingNumber, String lastScan, String location) {
    }

    private final RestClient restClient;

    public CarrierClient(RestClient.Builder builder, CarrierProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    public TrackingInfo track(String trackingNumber) {
        try {
            return restClient.get()
                    .uri("/tracking/{number}", trackingNumber)        // path variable: encoded, not concatenated
                    .retrieve()
                    .body(TrackingInfo.class);
        } catch (RestClientException e) {
            return new TrackingInfo(trackingNumber, "UNAVAILABLE", "Tracking service unavailable, try later");
        }
    }
}
