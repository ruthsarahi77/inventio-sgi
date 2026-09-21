package com.ruth.inventio.service;

import com.ruth.inventio.config.DashboardConfig;
import com.ruth.inventio.dto.DashboardStatsResponse;
import com.ruth.inventio.exception.ReglaNegocioException;
import com.ruth.inventio.model.EstadoRegistro;
import com.ruth.inventio.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final VentaRepository ventas;
    private final VentaDetalleRepository detalles;
    private final ReciboRepository recibos;
    private final ProductoRepository productos;
    private final DashboardConfig config;
    private final Clock clock;

    public DashboardService(VentaRepository ventas, VentaDetalleRepository detalles, ReciboRepository recibos,
            ProductoRepository productos, DashboardConfig config, @Qualifier("dashboardClock") Clock clock) {
        this.ventas = ventas; this.detalles = detalles; this.recibos = recibos;
        this.productos = productos; this.config = config; this.clock = clock;
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public DashboardStatsResponse obtener(Integer year, Integer month) {
        LocalDate today = LocalDate.now(clock.withZone(config.getZonaHoraria()));
        YearMonth period = periodo(year, month, today);
        var inicio = period.atDay(1).atStartOfDay(config.getZonaHoraria()).toInstant();
        var fin = period.plusMonths(1).atDay(1).atStartOfDay(config.getZonaHoraria()).toInstant();
        var inicioHoy = today.atStartOfDay(config.getZonaHoraria()).toInstant();
        var finHoy = today.plusDays(1).atStartOfDay(config.getZonaHoraria()).toInstant();
        var sales = ventas.resumirDashboard(inicio, fin, inicioHoy, finHoy);
        var volume = detalles.resumirVolumenDashboard(inicio, fin);
        BigDecimal litros = config.isVolumenEnLitros() && volume.getSinVolumen() == 0
                ? volume.getVolumen() : null;
        BigDecimal meta = config.getMetaRepsol();
        BigDecimal cumplimiento = litros == null || meta == null ? null : meta.signum() == 0
                ? BigDecimal.ZERO : litros.multiply(new BigDecimal("100")).divide(meta, 2, RoundingMode.HALF_UP);
        BigDecimal pen = config.getMonedaUnica() == null ? null
                : config.getMonedaUnica() == DashboardConfig.Moneda.PEN ? dinero(sales.getTotal()) : dinero(BigDecimal.ZERO);
        BigDecimal usd = config.getMonedaUnica() == null ? null
                : config.getMonedaUnica() == DashboardConfig.Moneda.USD ? dinero(sales.getTotal()) : dinero(BigDecimal.ZERO);
        return new DashboardStatsResponse(litros, meta, cumplimiento, pen, usd, dinero(sales.getSaldo()),
                null, productos.countByEstado(EstadoRegistro.ACTIVO), sales.getVentasHoy(),
                recibos.contarHoyDashboard(inicio, fin, inicioHoy, finHoy));
    }

    private YearMonth periodo(Integer year, Integer month, LocalDate today) {
        if ((year == null) != (month == null)) {
            throw new ReglaNegocioException(HttpStatus.BAD_REQUEST, "year y month deben enviarse juntos o ambos omitirse.");
        }
        if (year == null) return YearMonth.from(today);
        if (year < 1 || year > 9999 || month < 1 || month > 12) {
            throw new ReglaNegocioException(HttpStatus.BAD_REQUEST, "year debe estar entre 1 y 9999 y month entre 1 y 12.");
        }
        return YearMonth.of(year, month);
    }

    private BigDecimal dinero(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
}
