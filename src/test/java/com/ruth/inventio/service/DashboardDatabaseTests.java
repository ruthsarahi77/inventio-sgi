package com.ruth.inventio.service;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

/** Verificación optativa contra PostgreSQL existente, exclusivamente de lectura. */
@SpringBootTest(properties = {"spring.jpa.hibernate.ddl-auto=none", "spring.sql.init.mode=never",
        "inventio.bootstrap.enabled=false", "spring.datasource.hikari.read-only=true"})
@EnabledIfEnvironmentVariable(named = "INVENTIO_DASHBOARD_DB_TEST", matches = "true")
class DashboardDatabaseTests {
    @Autowired DashboardService service;

    @Test @WithMockUser(roles = "ADMIN")
    void aggregatesRealDatabaseAndExportsActualResponse() throws Exception {
        var stats = service.obtener(null, null);
        assertNotNull(stats.saldoPendienteTotal()); assertNotNull(stats.totalProductos());
        assertTrue(stats.saldoPendienteTotal().signum() >= 0);
        assertTrue(stats.totalProductos() >= 0); assertTrue(stats.ventasHoy() >= 0); assertTrue(stats.recibosHoy() >= 0);
        Files.writeString(Path.of("target/dashboard-example.json"),
                JsonMapper.builder().build().writerWithDefaultPrettyPrinter().writeValueAsString(stats));
        var previous = service.obtener(2020, 1);
        assertEquals(0L, previous.ventasHoy()); assertEquals(0L, previous.recibosHoy());
    }
}
