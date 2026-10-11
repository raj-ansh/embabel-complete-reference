package com.northwind.safety.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
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
import java.util.List;

/**
 * Chapter 84, lab profile only: tokens signed with a shared secret, so the examples run without an
 * identity provider. In production, set spring.security.oauth2.resourceserver.jwt.issuer-uri instead and
 * Spring Boot builds the decoder from the provider's published keys.
 */
@Configuration
@Profile("lab")
public class LabTokens {

    private final SecretKey key;

    public LabTokens(SecurityProperties properties) {
        this.key = new SecretKeySpec(properties.labJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtDecoder labJwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    /** Mints a token, as the identity provider would. */
    public String tokenFor(String user, String tenant, String... roles) {
        var claims = JwtClaimsSet.builder()
                .subject(user)
                .claim("tenant", tenant)
                .claim("roles", List.of(roles))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        return new NimbusJwtEncoder(new ImmutableSecret<>(key)).encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
