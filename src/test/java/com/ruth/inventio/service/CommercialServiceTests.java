package com.ruth.inventio.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import com.ruth.inventio.dto.*;
import com.ruth.inventio.entity.*;
import com.ruth.inventio.exception.*;
import com.ruth.inventio.model.*;
import com.ruth.inventio.repository.*;
import com.ruth.inventio.security.CurrentUser;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommercialServiceTests {
    private static ValidatorFactory validation;
    private ProductoRepository productos;
    private ClienteRepository clientes;
    private VentaRepository ventas;
    private VentaDetalleRepository detalles;
    private ProformaRepository quotes;
    private ProformaDetalleRepository quoteDetalles;
    private ReciboRepository recibos;
    private InventoryService inventory;
    private CurrentUser current;
    private SalesService sales;
    private ReceiptService receipts;
    private QuoteService quoteService;
    private Producto producto;
    private Cliente cliente;
    private Usuario vendedor;
    private Venta saved;

    @BeforeAll static void init() { validation=Validation.buildDefaultValidatorFactory(); }
    @AfterAll static void close() { validation.close(); }

    private <T extends EntidadCreada> T id(T entity,long id) {
        ReflectionTestUtils.setField(entity,"id",id); return entity;
    }
    @BeforeEach void setup() {
        productos=mock(ProductoRepository.class); clientes=mock(ClienteRepository.class);
        ventas=mock(VentaRepository.class); detalles=mock(VentaDetalleRepository.class);
        quotes=mock(ProformaRepository.class); quoteDetalles=mock(ProformaDetalleRepository.class);
        recibos=mock(ReciboRepository.class); inventory=mock(InventoryService.class); current=mock(CurrentUser.class);
        vendedor=id(new Usuario(),7); vendedor.setNombre("Vendedor");
        cliente=id(new Cliente(),2); cliente.setNombre("Cliente");
        producto=id(new Producto(),1); producto.setCodigo("P1"); producto.setNombre("Producto");
        producto.setCostoUnitario(new BigDecimal("1.00"));
        when(current.usuario()).thenReturn(vendedor); when(current.id()).thenReturn(7L);
        when(clientes.findById(2L)).thenReturn(Optional.of(cliente));
        when(productos.findById(1L)).thenReturn(Optional.of(producto));
        when(ventas.save(any())).thenAnswer(a -> {
            Venta v=a.getArgument(0); if(v.getId()==null) id(v,10); saved=v; return v;
        });
        when(detalles.save(any())).thenAnswer(a -> id(a.getArgument(0),11));
        when(quotes.save(any())).thenAnswer(a -> id(a.getArgument(0),20));
        when(quoteDetalles.save(any())).thenAnswer(a -> id(a.getArgument(0),21));
        when(recibos.save(any())).thenAnswer(a -> id(a.getArgument(0),30));
        var calculator=new DocumentoCalculator(productos,validation.getValidator());
        sales=new SalesService(ventas,detalles,clientes,quotes,recibos,inventory,calculator,current);
        receipts=new ReceiptService(recibos,ventas,current,validation.getValidator());
        quoteService=new QuoteService(quotes,quoteDetalles,clientes,ventas,calculator,current);
    }
    private VentaRequest ventaRequest() {
        return new VentaRequest(2L,null,List.of(new DetalleComercialRequest(1L,new BigDecimal("2"),new BigDecimal("10"))));
    }
    private Venta crearVenta() {
        sales.crear(ventaRequest());
        when(ventas.buscarParaActualizar(10L)).thenReturn(Optional.of(saved));
        return saved;
    }
    @Test void createsPendingSaleWithBackendAmountsAndAutomaticInventoryExit() {
        var result=sales.crear(ventaRequest());
        assertEquals(new BigDecimal("20.00"),result.total());
        assertEquals(result.total(),result.saldo());
        assertEquals(0,result.totalAbonado().signum());
        assertEquals(EstadoVenta.PENDIENTE,result.estado());
        assertEquals(7L,result.vendedorId());
        verify(inventory).bloquearProductos(List.of(1L));
        verify(inventory).registrarSalida(argThat(r -> r.productoId()==1L && r.usuarioId()==7L
                && r.cantidad().compareTo(new BigDecimal("2"))==0 && r.documentoOrigen().equals(result.numero())));
    }
    @Test void rejectsInsufficientStockThroughInventoryService() {
        when(inventory.registrarSalida(any())).thenThrow(new StockInsuficienteException(1L,BigDecimal.ZERO,BigDecimal.TWO));
        assertThrows(StockInsuficienteException.class,() -> sales.crear(ventaRequest()));
    }
    @Test void firstReceiptIsPartialAndFinalReceiptIsPaid() {
        var venta=crearVenta();
        when(recibos.sumarAbonos(10L)).thenReturn(BigDecimal.ZERO,new BigDecimal("5"),new BigDecimal("5"),new BigDecimal("20"));
        receipts.crear(new ReciboRequest(10L,new BigDecimal("5"),null));
        assertEquals(EstadoVenta.PARCIAL,venta.getEstado());
        assertEquals(new BigDecimal("15.00"),venta.getSaldo());
        receipts.crear(new ReciboRequest(10L,new BigDecimal("15"),null));
        assertEquals(EstadoVenta.PAGADA,venta.getEstado());
        assertEquals(0,venta.getSaldo().signum());
        verify(recibos,times(4)).sumarAbonos(10L);
    }
    @Test void rejectsOverpaymentWithoutCreatingReceipt() {
        crearVenta(); when(recibos.sumarAbonos(10L)).thenReturn(new BigDecimal("5"));
        assertThrows(ReglaNegocioException.class,() -> receipts.crear(new ReciboRequest(10L,new BigDecimal("15.01"),null)));
        verify(recibos,never()).save(any());
    }
    @Test void cancelsWithCompensationAndRejectsSecondCancellation() {
        crearVenta(); var result=sales.anular(10L);
        assertEquals(EstadoVenta.ANULADA,result.estado());
        verify(inventory).registrarAjusteEntrada(argThat(r -> r.cantidad().compareTo(new BigDecimal("2"))==0
                && r.documentoOrigen().equals(result.numero())));
        assertThrows(ReglaNegocioException.class,() -> sales.anular(10L));
        verify(inventory,times(1)).registrarAjusteEntrada(any());
    }
    @Test void cannotCancelPaidSaleOrPayCancelledSale() {
        var venta=crearVenta(); when(recibos.existsByVentaId(10L)).thenReturn(true);
        assertThrows(ReglaNegocioException.class,() -> sales.anular(10L));
        verify(inventory,never()).registrarAjusteEntrada(any());
        venta.setEstado(EstadoVenta.ANULADA);
        assertThrows(ReglaNegocioException.class,() -> receipts.crear(new ReciboRequest(10L,BigDecimal.ONE,null)));
        verify(recibos,never()).save(any());
    }
    @Test void detectsDuplicateProductCode() {
        when(productos.findByCodigo("P1")).thenReturn(Optional.of(producto));
        var service=new ProductService(productos);
        assertThrows(CodigoDuplicadoException.class,() -> service.crear(new ProductoRequest(
                "P1","Otro",null,null,null,null,BigDecimal.ONE)));
        verify(productos,never()).save(any());
    }
    @Test void quoteDoesNotChangeInventoryAndCanBeConvertedOnlyOnce() {
        var quote=quoteService.crear(new ProformaRequest(2L,ventaRequest().detalles(),null));
        assertEquals(new BigDecimal("20.00"),quote.total());
        verifyNoInteractions(inventory);
        Proforma p=id(new Proforma(),20); p.setCliente(cliente); p.setUsuario(vendedor);
        ProformaDetalle d=new ProformaDetalle(); d.setProducto(producto); d.setCantidad(BigDecimal.TWO);
        d.setPrecioUnitario(BigDecimal.TEN); p.getDetalles().add(d);
        when(quotes.buscarParaActualizar(20L)).thenReturn(Optional.of(p));
        assertEquals(20L,sales.crear(new VentaRequest(null,20L,null)).proformaId());
        when(ventas.existsByProformaId(20L)).thenReturn(true);
        assertThrows(ReglaNegocioException.class,() -> sales.crear(new VentaRequest(null,20L,null)));
    }
    @Test void sellerQueriesAreScopedToOwnDocuments() {
        when(current.soloVendedor()).thenReturn(true);
        sales.listar(); quoteService.listar(); receipts.listar();
        verify(ventas).findByVendedorId(7L); verify(quotes).findByUsuarioId(7L); verify(recibos).findByVentaVendedorId(7L);
        verify(ventas,never()).findAll(); verify(quotes,never()).findAll(); verify(recibos,never()).findAll();
    }
    @Test void validatesDetailsAndRoundsEachSubtotal() {
        var calc=new DocumentoCalculator(productos,validation.getValidator());
        assertThrows(ReglaNegocioException.class,() -> calc.calcular(List.of()));
        assertThrows(jakarta.validation.ConstraintViolationException.class,() -> calc.calcular(List.of(
                new DetalleComercialRequest(1L,BigDecimal.ONE,BigDecimal.ZERO))));
        var lines=calc.calcular(List.of(new DetalleComercialRequest(1L,new BigDecimal("0.125"),new BigDecimal("2.35"))));
        assertEquals(new BigDecimal("0.29"),calc.total(lines));
    }
}
