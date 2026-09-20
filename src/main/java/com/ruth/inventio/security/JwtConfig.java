package com.ruth.inventio.security;

import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

@Configuration
public class JwtConfig {
    @Bean
    SecretKey jwtSecretKey(@Value("${inventio.jwt.secret}") String encoded) {
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalStateException("Configurar JWT_SECRET: Base64 de al menos 32 bytes aleatorios.");
        }
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(encoded); }
        catch (IllegalArgumentException ex) { throw new IllegalStateException("JWT_SECRET debe ser Base64."); }
        if (bytes.length < 32) throw new IllegalStateException("JWT_SECRET requiere al menos 32 bytes aleatorios.");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSecretKey).algorithm(MacAlgorithm.HS256).build();
    }

    @Bean JwtDecoder jwtDecoder(SecretKey jwtSecretKey,
            @Value("${inventio.jwt.issuer}") String issuer,
            @Value("${inventio.jwt.audience}") String audience) {
        var decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new JwtClaimValidator<java.util.List<String>>("aud", aud -> aud != null && aud.contains(audience)),
                new JwtClaimValidator<java.time.Instant>("exp", java.util.Objects::nonNull)));
        return decoder;
    }
}
