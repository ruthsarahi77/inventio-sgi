package com.ruth.inventio.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.ruth.inventio.dto.MovimientoInventarioRequest;
import com.ruth.inventio.entity.MovimientoInventario;
import com.ruth.inventio.entity.Producto;
import com.ruth.inventio.entity.Usuario;
import com.ruth.inventio.exception.RecursoNoEncontradoException;
import com.ruth.inventio.exception.StockInsuficienteException;
import com.ruth.inventio.model.TipoMovimiento;
import com.ruth.inventio.repository.MovimientoInventarioRepository;
import com.ruth.inventio.repository.ProductoRepository;
import com.ruth.inventio.repository.StockProductoProjection;
import com.ruth.inventio.repository.UsuarioRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventoryServiceTests {

    private static ValidatorFactory validation;
    private ProductoRepository productos;
    private MovimientoInventarioRepository movimientos;
    private UsuarioRepository usuarios;
    private InventoryService service;
    private Producto producto;

    @BeforeAll
    static void createValidation() {
        validation = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidation() {
        validation.close();
    }

    @BeforeEach
    void prepare() {
        productos = mock(ProductoRepository.class);
        movimientos = mock(MovimientoInventarioRepository.class);
        usuarios = mock(UsuarioRepository.class);
        service = new InventoryService(productos, movimientos, usuarios, validation.getValidator());
        producto = new Producto();
        ReflectionTestUtils.setField(producto, "id", 1L);
        producto.setCodigo("P001");
        producto.setNombre("Producto de prueba");
    }

    private MovimientoInventarioRequest request(String cantidad) {
        return new MovimientoInventarioRequest(1L, new BigDecimal(cantidad), null, null, null);
    }

    private void stock(String cantidad) {
        when(productos.buscarParaActualizar(1L)).thenReturn(Optional.of(producto));
        when(movimientos.calcularStock(1L)).thenReturn(new BigDecimal(cantidad));
        when(movimientos.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @ParameterizedTest
    @EnumSource(TipoMovimiento.class)
    void locksBeforeReadingAndUsesCorrectSignForEveryMovement(TipoMovimiento tipo) {
        stock("10");
        var input = request("2.500");
        var result = switch (tipo) {
            case ENTRADA -> service.registrarEntrada(input);
            case AJUSTE_ENTRADA -> service.registrarAjusteEntrada(input);
            case SALIDA -> service.registrarSalida(input);
            case AJUSTE_SALIDA -> service.registrarAjusteSalida(input);
        };
        BigDecimal expected = switch (tipo) {
            case ENTRADA, AJUSTE_ENTRADA -> new BigDecimal("12.5");
            case SALIDA, AJUSTE_SALIDA -> new BigDecimal("7.5");
        };
        assertEquals(0, expected.compareTo(result.saldoAcumulado()));
        assertEquals(tipo, result.tipoMovimiento());
        assertNotNull(result.fecha());
        assertNull(result.usuario());
        var order = inOrder(productos, movimientos);
        order.verify(productos).buscarParaActualizar(1L);
        order.verify(movimientos).calcularStock(1L);
        order.verify(movimientos).obtenerUltimaFecha(1L);
        order.verify(movimientos).save(any());
        verify(productos, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = TipoMovimiento.class, names = {"SALIDA", "AJUSTE_SALIDA"})
    void rejectsOverdrawAndAcceptsExactAvailableStock(TipoMovimiento tipo) {
        stock("2");
        assertThrows(StockInsuficienteException.class, () -> {
            if (tipo == TipoMovimiento.SALIDA) service.registrarSalida(request("2.001"));
            else service.registrarAjusteSalida(request("2.001"));
        });
        verify(movimientos, never()).save(any());
        var result = tipo == TipoMovimiento.SALIDA
                ? service.registrarSalida(request("2")) : service.registrarAjusteSalida(request("2"));
        assertEquals(0, result.saldoAcumulado().signum());
    }

    @Test
    void cannotWithdrawFromProductWithoutMovements() {
        stock("0");
        assertThrows(StockInsuficienteException.class, () -> service.registrarAjusteSalida(request("1")));
        verify(movimientos, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0.0001", "10000000000000000"})
    void validatesInternalCallsBeforeAccessingRepositories(String cantidad) {
        assertThrows(ConstraintViolationException.class, () -> service.registrarEntrada(request(cantidad)));
        verifyNoInteractions(productos, movimientos, usuarios);
    }

    @Test
    void missingProductAndUnknownUserDoNotInsertMovement() {
        assertThrows(RecursoNoEncontradoException.class, () -> service.registrarEntrada(request("1")));
        stock("0");
        assertThrows(RecursoNoEncontradoException.class, () -> service.registrarEntrada(
                new MovimientoInventarioRequest(1L, BigDecimal.ONE, 99L, null, null)));
        verify(movimientos, never()).save(any());
        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerStockProducto(99L));
        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerKardexProducto(99L));
    }

    @Test
    void preservesUserDocumentAndObservationAndAppendsAfterLatestDate() {
        stock("0");
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 7L);
        usuario.setNombre("Operador");
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));
        Instant ultima = Instant.parse("2100-01-01T00:00:00Z");
        when(movimientos.obtenerUltimaFecha(1L)).thenReturn(ultima);
        var result = service.registrarEntrada(new MovimientoInventarioRequest(
                1L, BigDecimal.ONE, 7L, "COMPRA-1", "Recepcion"));
        assertEquals(7L, result.usuario().id());
        assertEquals("COMPRA-1", result.documentoOrigen());
        assertEquals("Recepcion", result.observacion());
        assertTrue(result.fecha().isAfter(ultima));
    }

    @Test
    void stockValuationUsesOneAggregateQueryAndExactDecimalArithmetic() {
        StockProductoProjection row = mock(StockProductoProjection.class);
        when(row.getIdProducto()).thenReturn(1L);
        when(row.getStockActual()).thenReturn(new BigDecimal("0.125"));
        when(row.getCostoUnitario()).thenReturn(new BigDecimal("2.35"));
        when(productos.consultarStocks()).thenReturn(List.of(row));
        var result = service.obtenerStockActual();
        assertEquals(new BigDecimal("0.29375"), result.getFirst().valorInventario());
        verify(productos).consultarStocks();
        verifyNoInteractions(movimientos);
    }

    @Test
    void kardexAccumulatesAllSignsAndSupportsEmptyHistory() {
        when(productos.findById(1L)).thenReturn(Optional.of(producto));
        var entrada = movement(TipoMovimiento.ENTRADA, "10");
        var ajusteEntrada = movement(TipoMovimiento.AJUSTE_ENTRADA, "2");
        var salida = movement(TipoMovimiento.SALIDA, "3");
        var ajusteSalida = movement(TipoMovimiento.AJUSTE_SALIDA, "1");
        when(movimientos.findByProductoIdOrderByFechaAscIdAsc(1L))
                .thenReturn(List.of(entrada, ajusteEntrada, salida, ajusteSalida));
        var result = service.obtenerKardexProducto(1L);
        assertEquals(List.of(new BigDecimal("10"), new BigDecimal("12"), new BigDecimal("9"),
                new BigDecimal("8")), result.movimientos().stream().map(m -> m.saldoAcumulado()).toList());
        assertEquals(new BigDecimal("8"), result.stockActual());
        when(movimientos.findByProductoIdOrderByFechaAscIdAsc(1L)).thenReturn(List.of());
        assertEquals(BigDecimal.ZERO, service.obtenerKardexProducto(1L).stockActual());
    }

    private MovimientoInventario movement(TipoMovimiento tipo, String cantidad) {
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProducto(producto);
        movimiento.setTipoMovimiento(tipo);
        movimiento.setCantidad(new BigDecimal(cantidad));
        movimiento.setFecha(Instant.now());
        return movimiento;
    }
}
