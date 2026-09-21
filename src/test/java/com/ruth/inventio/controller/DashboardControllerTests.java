package com.ruth.inventio.controller;

import com.ruth.inventio.config.DashboardConfig;
import com.ruth.inventio.dto.DashboardStatsResponse;
import com.ruth.inventio.exception.GlobalExceptionHandler;
import com.ruth.inventio.repository.*;
import com.ruth.inventio.service.DashboardService;
import java.math.BigDecimal;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DashboardControllerTests {
    @Test void preservesExactJsonKeysIncludingUnavailableMetricsAndPassesPeriod() throws Exception {
        var service = mock(DashboardService.class);
        var response = new DashboardStatsResponse(null, null, null, null, null, BigDecimal.ZERO, null, 0L, 0L, 0L);
        when(service.obtener(2026, 3)).thenReturn(response);
        var mvc = MockMvcBuilders.standaloneSetup(new DashboardController(service)).build();
        String json = mvc.perform(get("/api/dashboard/stats?year=2026&month=3"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsString();
        var tree = JsonMapper.builder().build().readTree(json);
        assertEquals(10, tree.size());
        for (String name : new String[]{"litrosVendidosMes", "metaRepsol", "cumplimientoMeta", "totalVentasMes",
                "totalVentasUSD", "saldoPendienteTotal", "productosStockBajo", "totalProductos", "ventasHoy", "recibosHoy"}) {
            assertTrue(tree.has(name), name);
        }
        assertTrue(tree.get("totalVentasUSD").isNull()); verify(service).obtener(2026, 3);
    }

    @Test void invalidHttpPeriodsUseGlobal400() throws Exception {
        var service = new DashboardService(mock(VentaRepository.class), mock(VentaDetalleRepository.class),
                mock(ReciboRepository.class), mock(ProductoRepository.class), new DashboardConfig(), Clock.systemUTC());
        var mvc = MockMvcBuilders.standaloneSetup(new DashboardController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        for (String query : new String[]{"year=2026", "month=3", "year=2026&month=13", "year=abc&month=1", "year=0&month=1"}) {
            mvc.perform(get("/api/dashboard/stats?" + query)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }
}
