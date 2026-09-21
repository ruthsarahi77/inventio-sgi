package com.ruth.inventio.config;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneId;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "inventio.dashboard")
public class DashboardConfig {
    @PositiveOrZero
    private BigDecimal metaRepsol;
    @NotNull
    private ZoneId zonaHoraria = ZoneId.of("America/Lima");
    // Solo configurar cuando toda la base comparte estas convenciones.
    private boolean volumenEnLitros;
    private Moneda monedaUnica;
    public enum Moneda { PEN, USD }

    @Bean("dashboardClock")
    Clock dashboardClock() { return Clock.systemUTC(); }
}
