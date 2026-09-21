package com.ruth.inventio.service;

import com.ruth.inventio.config.DashboardConfig;
import com.ruth.inventio.exception.ReglaNegocioException;
import com.ruth.inventio.model.EstadoRegistro;
import com.ruth.inventio.repository.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashboardServiceTests {
    private final VentaRepository ventas = mock(VentaRepository.class);
    private final VentaDetalleRepository detalles = mock(VentaDetalleRepository.class);
    private final ReciboRepository recibos = mock(ReciboRepository.class);
    private final ProductoRepository productos = mock(ProductoRepository.class);
    private final DashboardConfig config = new DashboardConfig();
    private final DashboardVentasProjection sales = mock(DashboardVentasProjection.class);
    private final DashboardVolumenProjection volume = mock(DashboardVolumenProjection.class);
    private DashboardService service;

    @BeforeEach void setup() {
        // En Lima todavía es 29/02, aunque en UTC ya es marzo.
        service = new DashboardService(ventas, detalles, recibos, productos, config,
                Clock.fixed(Instant.parse("2024-03-01T02:00:00Z"), ZoneOffset.UTC));
        when(ventas.resumirDashboard(any(), any(), any(), any())).thenReturn(sales);
        when(detalles.resumirVolumenDashboard(any(), any())).thenReturn(volume);
        when(sales.getTotal()).thenReturn(new BigDecimal("118.00"));
        when(sales.getSaldo()).thenReturn(new BigDecimal("18.00"));
        when(sales.getVentasHoy()).thenReturn(2L);
        when(volume.getVolumen()).thenReturn(new BigDecimal("25.500000"));
        when(volume.getSinVolumen()).thenReturn(0L);
        when(productos.countByEstado(EstadoRegistro.ACTIVO)).thenReturn(3L);
        when(recibos.contarHoyDashboard(any(), any(), any(), any())).thenReturn(1L);
    }

    @Test void usesCalendarMonthInConfiguredZoneAndDoesNotInventMissingMetrics() {
        var result = service.obtener(null, null);
        verify(ventas).resumirDashboard(Instant.parse("2024-02-01T05:00:00Z"), Instant.parse("2024-03-01T05:00:00Z"),
                Instant.parse("2024-02-29T05:00:00Z"), Instant.parse("2024-03-01T05:00:00Z"));
        assertNull(result.litrosVendidosMes()); assertNull(result.metaRepsol());
        assertNull(result.cumplimientoMeta()); assertNull(result.totalVentasMes());
        assertNull(result.totalVentasUSD()); assertNull(result.productosStockBajo());
        assertEquals(new BigDecimal("18.00"), result.saldoPendienteTotal());
        assertEquals(3L, result.totalProductos()); assertEquals(2L, result.ventasHoy());
        assertEquals(1L, result.recibosHoy());
    }

    @Test void selectsExplicitMonthIncludingYearBoundary() {
        service.obtener(2025, 12);
        verify(detalles).resumirVolumenDashboard(Instant.parse("2025-12-01T05:00:00Z"), Instant.parse("2026-01-01T05:00:00Z"));
    }

    @Test void rejectsInvalidOrIncompletePeriodBeforeQuerying() {
        for (Integer[] input : new Integer[][]{{2026, null}, {null, 3}, {2026, 0}, {2026, 13}, {0, 1}, {10000, 1}}) {
            var error = assertThrows(ReglaNegocioException.class, () -> service.obtener(input[0], input[1]));
            assertEquals(400, error.getStatus().value());
        }
        verifyNoInteractions(ventas, detalles, recibos, productos);
    }

    @Test void usesExplicitConventionsAndRoundsPercentageWithoutConvertingMoney() {
        config.setMonedaUnica(DashboardConfig.Moneda.PEN); config.setVolumenEnLitros(true);
        config.setMetaRepsol(new BigDecimal("30"));
        var result = service.obtener(null, null);
        assertEquals(new BigDecimal("85.00"), result.cumplimientoMeta());
        assertEquals(new BigDecimal("118.00"), result.totalVentasMes());
        assertEquals(new BigDecimal("0.00"), result.totalVentasUSD());
        config.setMonedaUnica(DashboardConfig.Moneda.USD);
        result = service.obtener(null, null);
        assertEquals(new BigDecimal("0.00"), result.totalVentasMes());
        assertEquals(new BigDecimal("118.00"), result.totalVentasUSD());
        config.setMetaRepsol(new BigDecimal("7"));
        assertEquals(new BigDecimal("364.29"), service.obtener(null, null).cumplimientoMeta());
        config.setMetaRepsol(BigDecimal.ZERO);
        assertEquals(BigDecimal.ZERO, service.obtener(null, null).cumplimientoMeta());
    }

    @Test void missingVolumeDoesNotReturnPartialLiterTotal() {
        config.setVolumenEnLitros(true); config.setMetaRepsol(BigDecimal.TEN);
        when(volume.getSinVolumen()).thenReturn(1L);
        var result = service.obtener(null, null);
        assertNull(result.litrosVendidosMes()); assertNull(result.cumplimientoMeta());
    }
}
