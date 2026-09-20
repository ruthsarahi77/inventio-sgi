package com.ruth.inventio.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.ruth.inventio.dto.*;
import com.ruth.inventio.entity.*;
import com.ruth.inventio.exception.*;
import com.ruth.inventio.mapper.ComercialMapper;
import com.ruth.inventio.model.*;
import com.ruth.inventio.repository.*;
import com.ruth.inventio.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class QuoteService {
    private final ProformaRepository quotes;
    private final ProformaDetalleRepository detalles;
    private final ClienteRepository clientes;
    private final VentaRepository ventas;
    private final DocumentoCalculator calculator;
    private final CurrentUser current;
    public QuoteService(ProformaRepository quotes,ProformaDetalleRepository detalles,ClienteRepository clientes,
            VentaRepository ventas,DocumentoCalculator calculator,CurrentUser current) {
        this.quotes=quotes; this.detalles=detalles; this.clientes=clientes;
        this.ventas=ventas; this.calculator=calculator; this.current=current;
    }
    public List<ProformaResponse> listar() {
        return (current.soloVendedor()?quotes.findByUsuarioId(current.id()):quotes.findAll())
                .stream().map(ComercialMapper::proforma).toList();
    }
    public ProformaResponse obtener(Long id) {
        var p=quotes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Proforma no encontrada: "+id));
        current.verificarPropietario(p.getUsuario().getId());
        return ComercialMapper.proforma(p);
    }
    @Transactional @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ProformaResponse crear(ProformaRequest r) {
        var cliente=clientes.findById(r.clienteId()).orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado."));
        if (cliente.getEstado()!=EstadoRegistro.ACTIVO) throw new ReglaNegocioException("Cliente inactivo.");
        var lineas=calculator.calcular(r.detalles());
        var p=new Proforma();
        p.setCliente(cliente); p.setUsuario(current.usuario()); p.setNumero("PRO-"+UUID.randomUUID());
        p.setFecha(Instant.now()); p.setObservacion(r.observacion()); p.setTotal(calculator.total(lineas));
        p=quotes.save(p);
        for (var linea:lineas) {
            var d=new ProformaDetalle(); d.setProforma(p); d.setProducto(linea.producto());
            d.setCantidad(linea.cantidad()); d.setPrecioUnitario(linea.precio()); d.setSubtotal(linea.subtotal());
            p.getDetalles().add(detalles.save(d));
        }
        return ComercialMapper.proforma(p);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasRole('ADMIN')")
    public ProformaResponse anular(Long id) {
        var p=quotes.buscarParaActualizar(id).orElseThrow(() -> new RecursoNoEncontradoException("Proforma no encontrada."));
        if (p.getEstado()==EstadoProforma.ANULADA) throw new ReglaNegocioException("La proforma ya esta anulada.");
        if (ventas.existsByProformaId(id)) throw new ReglaNegocioException("La proforma ya esta asociada a una venta.");
        p.setEstado(EstadoProforma.ANULADA);
        return ComercialMapper.proforma(quotes.save(p));
    }
}
