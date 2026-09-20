package com.ruth.inventio.entity;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;

class DomainValidationTests {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void prepareValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "0", "0.0001", "10000000000000000"})
    void rejectsInvalidQuantities(String quantity) {
        for (Class<?> entity : List.of(MovimientoInventario.class, ProformaDetalle.class, VentaDetalle.class)) {
            assertFalse(validator.validateValue(entity, "cantidad", new BigDecimal(quantity)).isEmpty());
        }
    }

    @Test
    void acceptsFractionalPositiveQuantitiesAndRejectsMissingOnes() {
        for (Class<?> entity : List.of(MovimientoInventario.class, ProformaDetalle.class, VentaDetalle.class)) {
            assertTrue(validator.validateValue(entity, "cantidad", new BigDecimal("0.125")).isEmpty());
            assertFalse(validator.validateValue(entity, "cantidad", null).isEmpty());
        }
    }

    @Test
    void validatesMoneyWithoutRoundingOrUsingFloatingPoint() {
        assertFalse(validator.validateValue(Producto.class, "costoUnitario", new BigDecimal("-0.01")).isEmpty());
        assertFalse(validator.validateValue(Producto.class, "costoUnitario", new BigDecimal("1.001")).isEmpty());
        assertFalse(validator.validateValue(Producto.class, "costoUnitario", null).isEmpty());
        assertTrue(validator.validateValue(Producto.class, "costoUnitario", BigDecimal.ZERO).isEmpty());
        assertFalse(validator.validateValue(Recibo.class, "monto", BigDecimal.ZERO).isEmpty());
        assertTrue(validator.validateValue(Recibo.class, "monto", new BigDecimal("0.01")).isEmpty());
    }

    @Test
    void requiresSellerSaleAndAtLeastOneRole() {
        assertFalse(validator.validateValue(Venta.class, "vendedor", null).isEmpty());
        assertFalse(validator.validateValue(Recibo.class, "venta", null).isEmpty());
        assertFalse(validator.validateProperty(new Usuario(), "roles").isEmpty());
    }

    @Test
    void callbacksMaintainCreationAndUpdateTimestamps() {
        Producto producto = new Producto();
        producto.registrarCreacion();
        producto.registrarActualizacionInicial();
        var createdAt = producto.getCreatedAt();
        assertNotNull(createdAt);
        assertEquals(createdAt, producto.getUpdatedAt());
        producto.registrarActualizacion();
        assertEquals(createdAt, producto.getCreatedAt());
        assertFalse(producto.getUpdatedAt().isBefore(createdAt));
    }

    @Test
    void serializationOmitsPasswordAndRelationshipsEvenWithCycles() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Vendedor");
        usuario.setPassword("hash-de-prueba");
        Venta venta = new Venta();
        venta.setVendedor(usuario);
        usuario.getVentas().add(venta);
        JsonMapper mapper = JsonMapper.builder().build();

        var json = mapper.readTree(mapper.writeValueAsString(usuario));
        assertFalse(json.has("password"));
        assertFalse(json.has("ventas"));
        assertEquals("Vendedor", json.path("nombre").asString());
        assertFalse(mapper.readTree(mapper.writeValueAsString(venta)).has("vendedor"));
    }
}
