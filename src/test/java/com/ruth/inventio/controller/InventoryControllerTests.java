package com.ruth.inventio.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.ruth.inventio.dto.KardexResponse;
import com.ruth.inventio.dto.MovimientoInventarioResponse;
import com.ruth.inventio.dto.StockProductoResponse;
import com.ruth.inventio.exception.GlobalExceptionHandler;
import com.ruth.inventio.exception.RecursoNoEncontradoException;
import com.ruth.inventio.exception.StockInsuficienteException;
import com.ruth.inventio.model.TipoMovimiento;
import com.ruth.inventio.security.SecurityConfig;
import com.ruth.inventio.service.InventoryApiService;
import com.ruth.inventio.security.JwtUserConverter;
import com.ruth.inventio.security.SecurityErrorHandler;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InventoryControllerTests {

    private static AnnotationConfigWebApplicationContext context;
    private static InventoryApiService service;
    private static MockMvc mvc;

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({InventoryController.class, GlobalExceptionHandler.class, SecurityConfig.class, SecurityErrorHandler.class})
    static class TestConfig {
        @Bean
        InventoryApiService inventoryService() {
            return mock(InventoryApiService.class);
        }
        @Bean JwtDecoder jwtDecoder() { return mock(JwtDecoder.class); }
        @Bean JwtUserConverter jwtUserConverter() { return mock(JwtUserConverter.class); }
    }

    @BeforeAll
    static void prepareMvcWithRealSecurityFilters() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestConfig.class);
        context.refresh();
        service = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(context.getBean(InventoryApiService.class));
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity()).defaultRequest(get("/").with(user("admin").roles("ADMIN"))).build();
    }

    @AfterAll
    static void closeContext() {
        if (context != null) context.close();
    }

    @BeforeEach
    void clearService() {
        reset(service);
    }

    @Test
    void getsStockListProductAndKardexAsDtos() throws Exception {
        var stock = new StockProductoResponse(1L, "P001", "Producto", null, "unidad",
                new BigDecimal("2.50"), new BigDecimal("10"), new BigDecimal("25.00"));
        when(service.obtenerStockActual()).thenReturn(List.of(stock));
        when(service.obtenerStockProducto(1L)).thenReturn(stock);
        when(service.obtenerKardexProducto(1L)).thenReturn(new KardexResponse(1L, "P001", "Producto",
                BigDecimal.ZERO, List.of()));
        mvc.perform(get("/api/inventory")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].valorInventario").value(25));
        mvc.perform(get("/api/inventory/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.idProducto").value(1));
        mvc.perform(get("/api/inventory/kardex/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.movimientos").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"entries", "adjustments/in", "adjustments/out"})
    void postsWithoutCsrfTokenAndDelegatesToCorrectOperation(String endpoint) throws Exception {
        TipoMovimiento tipo = switch (endpoint) {
            case "entries" -> TipoMovimiento.ENTRADA;
            case "adjustments/in" -> TipoMovimiento.AJUSTE_ENTRADA;
            default -> TipoMovimiento.AJUSTE_SALIDA;
        };
        var result = new MovimientoInventarioResponse(1L, 1L, tipo, BigDecimal.ONE, Instant.now(),
                null, null, null, BigDecimal.TEN);
        when(service.registrarEntrada(any())).thenReturn(result);
        when(service.registrarAjusteEntrada(any())).thenReturn(result);
        when(service.registrarAjusteSalida(any())).thenReturn(result);
        mvc.perform(post("/api/inventory/" + endpoint).contentType(MediaType.APPLICATION_JSON)
                .content("{\"productoId\":1,\"cantidad\":1}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.tipoMovimiento").value(tipo.name()));
        switch (tipo) {
            case ENTRADA -> verify(service).registrarEntrada(any());
            case AJUSTE_ENTRADA -> verify(service).registrarAjusteEntrada(any());
            case AJUSTE_SALIDA -> verify(service).registrarAjusteSalida(any());
            default -> throw new AssertionError();
        }
        verifyNoMoreInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "null", "0.0001"})
    void rejectsInvalidQuantityWithStandard400(String cantidad) throws Exception {
        mvc.perform(post("/api/inventory/entries").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productoId\":1,\"cantidad\":" + cantidad + "}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/inventory/entries"));
        verifyNoInteractions(service);
    }

    @Test
    void returns404And409WithStandardErrorBody() throws Exception {
        when(service.obtenerStockProducto(99L)).thenThrow(new RecursoNoEncontradoException("Producto no encontrado"));
        mvc.perform(get("/api/inventory/99")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        when(service.registrarAjusteSalida(any())).thenThrow(new StockInsuficienteException(1L,
                BigDecimal.ONE, BigDecimal.TEN));
        mvc.perform(post("/api/inventory/adjustments/out").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productoId\":1,\"cantidad\":10}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void doesNotExposeManualExitUpdateDeleteOrSales() throws Exception {
        mvc.perform(post("/api/inventory/exits")).andExpect(status().isForbidden());
        mvc.perform(put("/api/inventory/1")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/inventory/1")).andExpect(status().isForbidden());
        mvc.perform(get("/api/unknown")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
