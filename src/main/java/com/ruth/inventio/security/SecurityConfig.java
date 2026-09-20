package com.ruth.inventio.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder decoder,
            JwtUserConverter converter, SecurityErrorHandler errors) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers("/api/users/**", "/api/roles/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/inventory/kardex/**").hasAnyRole("ADMIN", "SUPERVISOR")
                .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/customers/**", "/api/inventory/**",
                        "/api/quotes/**", "/api/sales/**", "/api/receipts/**")
                    .hasAnyRole("ADMIN", "SUPERVISOR", "VENDEDOR")
                .requestMatchers(HttpMethod.POST, "/api/customers", "/api/quotes", "/api/sales", "/api/receipts")
                    .hasAnyRole("ADMIN", "VENDEDOR")
                .requestMatchers(HttpMethod.PUT, "/api/customers/{id}").hasAnyRole("ADMIN", "VENDEDOR")
                .requestMatchers(HttpMethod.POST, "/api/products", "/api/inventory/entries",
                        "/api/inventory/adjustments/in", "/api/inventory/adjustments/out").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/products/{id}").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/products/{id}/status", "/api/quotes/{id}/cancel",
                        "/api/sales/{id}/cancel").hasRole("ADMIN")
                .anyRequest().denyAll());
        // Solo Bearer explicito, sin cookies de autenticacion ni sesion de servidor.
        http.csrf(csrf -> csrf.disable());
        http.cors(cors -> {});
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.requestCache(cache -> cache.disable());
        http.exceptionHandling(ex -> ex.authenticationEntryPoint(errors).accessDeniedHandler(errors));
        http.oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.decoder(decoder).jwtAuthenticationConverter(converter))
                .authenticationEntryPoint(errors).accessDeniedHandler(errors));
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${CORS_ALLOWED_ORIGINS:}") String origins) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
