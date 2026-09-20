package com.ruth.inventio.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.ruth.inventio.dto.KardexResponse;
import com.ruth.inventio.dto.MovimientoInventarioRequest;
import com.ruth.inventio.dto.MovimientoInventarioResponse;
import com.ruth.inventio.dto.StockProductoResponse;
import com.ruth.inventio.dto.UsuarioInventarioResponse;
import com.ruth.inventio.entity.MovimientoInventario;
import com.ruth.inventio.entity.Usuario;
import com.ruth.inventio.exception.RecursoNoEncontradoException;
import com.ruth.inventio.exception.StockInsuficienteException;
import com.ruth.inventio.model.TipoMovimiento;
import com.ruth.inventio.repository.MovimientoInventarioRepository;
import com.ruth.inventio.repository.ProductoRepository;
import com.ruth.inventio.repository.StockProductoProjection;
import com.ruth.inventio.repository.UsuarioRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InventoryService {

    private final ProductoRepository productos;
    private final MovimientoInventarioRepository movimientos;
    private final UsuarioRepository usuarios;
    private final Validator validator;

    public InventoryService(ProductoRepository productos, MovimientoInventarioRepository movimientos,
            UsuarioRepository usuarios, Validator validator) {
        this.productos = productos;
        this.movimientos = movimientos;
        this.usuarios = usuarios;
        this.validator = validator;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MovimientoInventarioResponse registrarEntrada(MovimientoInventarioRequest request) {
        return registrar(request, TipoMovimiento.ENTRADA);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MovimientoInventarioResponse registrarAjusteEntrada(MovimientoInventarioRequest request) {
        return registrar(request, TipoMovimiento.AJUSTE_ENTRADA);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MovimientoInventarioResponse registrarAjusteSalida(MovimientoInventarioRequest request) {
        return registrar(request, TipoMovimiento.AJUSTE_SALIDA);
    }

    /** Para futuros servicios internos, sin endpoint de salida manual.
     * La transaccion que invoque este metodo debe usar READ_COMMITTED.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MovimientoInventarioResponse registrarSalida(MovimientoInventarioRequest request) {
        return registrar(request, TipoMovimiento.SALIDA);
    }

    public List<StockProductoResponse> obtenerStockActual() {
        return productos.consultarStocks().stream().map(this::stockResponse).toList();
    }

    /** Orden global de bloqueo para ventas con varios productos. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void bloquearProductos(List<Long> ids) {
        ids.stream().distinct().sorted().forEach(id -> productos.buscarParaActualizar(id)
                .orElseThrow(() -> productoNoEncontrado(id)));
    }

    public StockProductoResponse obtenerStockProducto(Long productoId) {
        return productos.consultarStock(productoId).map(this::stockResponse)
                .orElseThrow(() -> productoNoEncontrado(productoId));
    }

    public KardexResponse obtenerKardexProducto(Long productoId) {
        var producto = productos.findById(productoId)
                .orElseThrow(() -> productoNoEncontrado(productoId));
        BigDecimal saldo = BigDecimal.ZERO;
        List<MovimientoInventarioResponse> detalle = new ArrayList<>();
        for (var movimiento : movimientos.findByProductoIdOrderByFechaAscIdAsc(productoId)) {
            saldo = saldo.add(cantidadFirmada(movimiento.getTipoMovimiento(), movimiento.getCantidad()));
            detalle.add(movimientoResponse(movimiento, saldo));
        }
        return new KardexResponse(productoId, producto.getCodigo(), producto.getNombre(), saldo, detalle);
    }

    private MovimientoInventarioResponse registrar(MovimientoInventarioRequest request, TipoMovimiento tipo) {
        // Tambien se valida al invocar desde otro servicio, no solo desde HTTP.
        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        // Bloquear una fila estable protege incluso productos sin movimientos.
        // Todas las escrituras de inventario deben pasar por esta misma secuencia.
        var producto = productos.buscarParaActualizar(request.productoId())
                .orElseThrow(() -> productoNoEncontrado(request.productoId()));
        BigDecimal disponible = movimientos.calcularStock(producto.getId());
        BigDecimal saldo = disponible.add(cantidadFirmada(tipo, request.cantidad()));
        if (saldo.signum() < 0) {
            throw new StockInsuficienteException(producto.getId(), disponible, request.cantidad());
        }

        Usuario usuario = request.usuarioId() == null ? null : usuarios.findById(request.usuarioId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + request.usuarioId()));

        // Se agrega al final del kardex, incluso si el reloj del servidor retrocede.
        Instant fecha = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Instant ultimaFecha = movimientos.obtenerUltimaFecha(producto.getId());
        if (ultimaFecha != null && !fecha.isAfter(ultimaFecha)) {
            fecha = ultimaFecha.plus(1, ChronoUnit.MICROS);
        }

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProducto(producto);
        movimiento.setTipoMovimiento(tipo);
        movimiento.setCantidad(request.cantidad());
        movimiento.setFecha(fecha);
        movimiento.setUsuario(usuario);
        movimiento.setDocumentoOrigen(request.documentoOrigen());
        movimiento.setObservacion(request.observacion());
        return movimientoResponse(movimientos.save(movimiento), saldo);
    }

    private BigDecimal cantidadFirmada(TipoMovimiento tipo, BigDecimal cantidad) {
        return switch (tipo) {
            case ENTRADA, AJUSTE_ENTRADA -> cantidad;
            case SALIDA, AJUSTE_SALIDA -> cantidad.negate();
        };
    }

    private StockProductoResponse stockResponse(StockProductoProjection stock) {
        return new StockProductoResponse(stock.getIdProducto(), stock.getCodigo(), stock.getNombre(),
                stock.getPresentacion(), stock.getUnidad(), stock.getCostoUnitario(),
                stock.getStockActual(), stock.getCostoUnitario().multiply(stock.getStockActual()));
    }

    private MovimientoInventarioResponse movimientoResponse(MovimientoInventario movimiento, BigDecimal saldo) {
        Usuario usuario = movimiento.getUsuario();
        return new MovimientoInventarioResponse(movimiento.getId(), movimiento.getProducto().getId(),
                movimiento.getTipoMovimiento(), movimiento.getCantidad(), movimiento.getFecha(),
                usuario == null ? null : new UsuarioInventarioResponse(usuario.getId(), usuario.getNombre()),
                movimiento.getDocumentoOrigen(), movimiento.getObservacion(), saldo);
    }

    private RecursoNoEncontradoException productoNoEncontrado(Long id) {
        return new RecursoNoEncontradoException("Producto no encontrado: " + id);
    }
}
