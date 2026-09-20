package com.ruth.inventio.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import com.ruth.inventio.dto.*;
import com.ruth.inventio.entity.*;
import com.ruth.inventio.exception.*;
import com.ruth.inventio.mapper.ComercialMapper;
import com.ruth.inventio.model.*;
import com.ruth.inventio.repository.*;
import com.ruth.inventio.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class SalesService {
    private final VentaRepository ventas;
    private final VentaDetalleRepository detalles;
    private final ClienteRepository clientes;
    private final ProformaRepository quotes;
    private final ReciboRepository recibos;
    private final InventoryService inventory;
    private final DocumentoCalculator calculator;
    private final CurrentUser current;
    public SalesService(VentaRepository ventas,VentaDetalleRepository detalles,ClienteRepository clientes,
            ProformaRepository quotes,ReciboRepository recibos,InventoryService inventory,
            DocumentoCalculator calculator,CurrentUser current) {
        this.ventas=ventas; this.detalles=detalles; this.clientes=clientes; this.quotes=quotes;
        this.recibos=recibos; this.inventory=inventory; this.calculator=calculator; this.current=current;
    }
    public List<VentaResponse> listar() {
        return (current.soloVendedor()?ventas.findByVendedorId(current.id()):ventas.findAll())
                .stream().map(ComercialMapper::venta).toList();
    }
    public VentaResponse obtener(Long id) {
        var v=ventas.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Venta no encontrada: "+id));
        current.verificarPropietario(v.getVendedor().getId());
        return ComercialMapper.venta(v);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public VentaResponse crear(VentaRequest request) {
        Long clienteId=request.clienteId();
        List<DetalleComercialRequest> solicitudes=request.detalles();
        Proforma proforma=null;
        if (request.proformaId()!=null) {
            if (clienteId!=null || solicitudes!=null) throw new ReglaNegocioException(HttpStatus.BAD_REQUEST,
                    "Envie solo proformaId para convertir una proforma.");
            proforma=quotes.buscarParaActualizar(request.proformaId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Proforma no encontrada."));
            current.verificarPropietario(proforma.getUsuario().getId());
            if (proforma.getEstado()!=EstadoProforma.EMITIDA) throw new ReglaNegocioException("Proforma anulada.");
            if (ventas.existsByProformaId(proforma.getId())) throw new ReglaNegocioException("Proforma ya convertida en venta.");
            clienteId=proforma.getCliente().getId();
            solicitudes=proforma.getDetalles().stream().map(d -> new DetalleComercialRequest(
                    d.getProducto().getId(),d.getCantidad(),d.getPrecioUnitario())).toList();
        }
        if (clienteId==null || solicitudes==null || solicitudes.isEmpty() || solicitudes.stream().anyMatch(java.util.Objects::isNull)) {
            throw new ReglaNegocioException(HttpStatus.BAD_REQUEST,"Cliente y detalles son obligatorios.");
        }
        var cliente=clientes.findById(clienteId).orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado."));
        if (cliente.getEstado()!=EstadoRegistro.ACTIVO) throw new ReglaNegocioException("Cliente inactivo.");
        var vendedor=current.usuario();
        calculator.validarDetalles(solicitudes);
        inventory.bloquearProductos(solicitudes.stream().map(DetalleComercialRequest::productoId).toList());
        var lineas=calculator.calcular(solicitudes);
        var venta=new Venta();
        venta.setNumero("VEN-"+UUID.randomUUID()); venta.setFecha(Instant.now());
        venta.setCliente(cliente); venta.setVendedor(vendedor); venta.setProforma(proforma);
        venta.setTotal(calculator.total(lineas)); venta.setTotalAbonado(new BigDecimal("0.00"));
        venta.setSaldo(venta.getTotal()); venta.setEstado(EstadoVenta.PENDIENTE);
        venta=ventas.save(venta);
        for (var linea:lineas) {
            var d=new VentaDetalle(); d.setVenta(venta); d.setProducto(linea.producto());
            d.setCantidad(linea.cantidad()); d.setPrecioUnitario(linea.precio()); d.setSubtotal(linea.subtotal());
            venta.getDetalles().add(detalles.save(d));
        }
        // InventoryService calcula disponibilidad; nunca hay una segunda formula de stock aqui.
        for (var linea:lineas.stream().sorted(Comparator.comparing(l -> l.producto().getId())).toList()) {
            inventory.registrarSalida(new MovimientoInventarioRequest(linea.producto().getId(),linea.cantidad(),
                    vendedor.getId(),venta.getNumero(),"Salida por venta"));
        }
        return ComercialMapper.venta(venta);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasRole('ADMIN')")
    public VentaResponse anular(Long id) {
        var venta=ventas.buscarParaActualizar(id).orElseThrow(() -> new RecursoNoEncontradoException("Venta no encontrada."));
        if (venta.getEstado()==EstadoVenta.ANULADA) throw new ReglaNegocioException("La venta ya esta anulada.");
        if (recibos.existsByVentaId(id) || venta.getTotalAbonado().signum()>0) {
            throw new ReglaNegocioException("No se puede anular una venta con recibos registrados.");
        }
        var actor=current.usuario();
        var lineas=venta.getDetalles().stream().sorted(Comparator.comparing(d -> d.getProducto().getId())).toList();
        inventory.bloquearProductos(lineas.stream().map(d -> d.getProducto().getId()).toList());
        for (var d:lineas) {
            inventory.registrarAjusteEntrada(new MovimientoInventarioRequest(d.getProducto().getId(),d.getCantidad(),
                    actor.getId(),venta.getNumero(),"Compensacion por anulacion de venta"));
        }
        venta.setEstado(EstadoVenta.ANULADA);
        return ComercialMapper.venta(ventas.save(venta));
    }
}
