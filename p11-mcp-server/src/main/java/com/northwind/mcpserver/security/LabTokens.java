package com.northwind.mcpserver.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Chapter 88, lab profile only: tokens signed with a shared secret, so the server can be tested without an
 * identity provider. In production, set spring.security.oauth2.resourceserver.jwt.issuer-uri instead.
 * Scopes in the "scope" claim become authorities such as "orders:read".
 */
@Configuration
@Profile("lab")
public class LabTokens {

    private final SecretKey key;

    public LabTokens(@Value("${northwind.security.lab-jwt-secret}") String secret) {
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtDecoder labJwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    public String tokenFor(String client, String scopes) {
        var claims = JwtClaimsSet.builder()
                .subject(client)
                .claim("scope", scopes)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        return new NimbusJwtEncoder(new ImmutableSecret<>(key))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
