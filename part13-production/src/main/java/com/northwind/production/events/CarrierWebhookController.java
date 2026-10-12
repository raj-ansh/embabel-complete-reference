package com.northwind.production.events;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Chapter 99: the carrier tells NorthWind about failed deliveries by webhook. The request is authenticated
 * with an HMAC signature over the raw body, turned into a domain event and acknowledged at once; the agent
 * runs later, outside the carrier's request.
 */
@RestController
public class CarrierWebhookController {

    private final ApplicationEventPublisher events;
    private final byte[] secret;
    private final JsonMapper json = JsonMapper.builder().build();

    public CarrierWebhookController(ApplicationEventPublisher events,
                                    @Value("${northwind.carrier.webhook-secret}") String secret) {
        this.events = events;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping("/webhooks/carrier")
    public ResponseEntity<Void> receive(@RequestHeader("X-Signature") String signature, @RequestBody byte[] body) {
        if (!MessageDigest.isEqual(sign(body).getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8))) {          // constant-time comparison
            return ResponseEntity.status(401).build();
        }
        events.publishEvent(json.readValue(body, DeliveryFailed.class));
        return ResponseEntity.accepted().build();
    }

    public String sign(byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
