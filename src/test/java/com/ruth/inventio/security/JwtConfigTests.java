package com.ruth.inventio.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class JwtConfigTests {
    private final JwtConfig config = new JwtConfig();

    @Test void rejectsMissingMalformedAndShortSecrets() {
        for (String secret : new String[]{"", " ", "not$base64", Base64.getEncoder().encodeToString(new byte[31])}) {
            var error = assertThrows(IllegalStateException.class, () -> config.jwtSecretKey(secret));
            assertTrue(error.getMessage().contains("JWT_SECRET"));
        }
    }

    @Test void applicationPropertiesConnectEnvironmentToJwtBeans() throws Exception {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        var properties = new Properties();
        try (var input = getClass().getResourceAsStream("/application.properties")) {
            assertNotNull(input);
            properties.load(input);
        }
        var environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().addLast(new PropertiesPropertySource("application", properties));
        assertEquals("", environment.getProperty("inventio.jwt.secret"));
        var variables = new Properties();
        variables.setProperty("JWT_SECRET", Base64.getEncoder().encodeToString(bytes));
        environment.getPropertySources().addFirst(new PropertiesPropertySource("variables", variables));
        assertArrayEquals(bytes, config.jwtSecretKey(environment.getRequiredProperty("inventio.jwt.secret")).getEncoded());
        assertEquals("inventio-api", environment.getProperty("inventio.jwt.issuer"));
        assertEquals("inventio-clients", environment.getProperty("inventio.jwt.audience"));
        assertEquals("900", environment.getProperty("inventio.jwt.ttl-seconds"));
        // Conservar la configuracion del proyecto; autenticacion no cambia el esquema.
        assertEquals("update", environment.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("never", environment.getProperty("spring.sql.init.mode"));
    }
}
