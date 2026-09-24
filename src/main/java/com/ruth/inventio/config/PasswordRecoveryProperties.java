package com.ruth.inventio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.util.Set;

@Component
public class PasswordRecoveryProperties {
    private final String frontendUrl;
    private final int ttlMinutes;
    public PasswordRecoveryProperties(@Value("${inventio.password-reset.frontend-url:}") String frontendUrl,
            @Value("${inventio.password-reset.ttl-minutes:20}") int ttlMinutes) {
        if (ttlMinutes < 15 || ttlMinutes > 30) throw new IllegalStateException("PASSWORD_RESET_TTL_MINUTES debe estar entre 15 y 30.");
        if (!frontendUrl.isBlank()) {
            try {
                URI uri = URI.create(frontendUrl);
                boolean secure = "https".equalsIgnoreCase(uri.getScheme());
                boolean local = "http".equalsIgnoreCase(uri.getScheme())
                        && Set.of("localhost", "127.0.0.1", "[::1]").contains(uri.getHost() == null ? "" : uri.getHost());
                if ((!secure && !local) || uri.getHost() == null || uri.getRawUserInfo() != null || uri.getFragment() != null) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException("FRONTEND_RESET_PASSWORD_URL debe ser HTTPS (HTTP solo localhost), sin credenciales ni fragmento.");
            }
        }
        this.frontendUrl = frontendUrl; this.ttlMinutes = ttlMinutes;
    }
    public boolean configured() { return !frontendUrl.isBlank(); }
    public int ttlMinutes() { return ttlMinutes; }
    public String link(String token) {
        return UriComponentsBuilder.fromUriString(frontendUrl).replaceQueryParam("token", token).build().encode().toUriString();
    }
}
