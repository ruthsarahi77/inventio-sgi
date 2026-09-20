package com.ruth.inventio.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.ruth.inventio.dto.*;
import com.ruth.inventio.entity.Recibo;
import com.ruth.inventio.exception.*;
import com.ruth.inventio.mapper.ComercialMapper;
import com.ruth.inventio.model.EstadoVenta;
import com.ruth.inventio.repository.*;
import com.ruth.inventio.security.CurrentUser;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class ReceiptService {
    private final ReciboRepository recibos;
    private final VentaRepository ventas;
    private final CurrentUser current;
    private final Validator validator;
    public ReceiptService(ReciboRepository recibos,VentaRepository ventas,CurrentUser current,Validator validator) {
        this.recibos=recibos; this.ventas=ventas; this.current=current; this.validator=validator;
    }
    public List<ReciboResponse> listar() {
        return (current.soloVendedor()?recibos.findByVentaVendedorId(current.id()):recibos.findAll())
                .stream().map(ComercialMapper::recibo).toList();
    }
    public ReciboResponse obtener(Long id) {
        var r=recibos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Recibo no encontrado."));
        current.verificarPropietario(r.getVenta().getVendedor().getId());
        return ComercialMapper.recibo(r);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ReciboResponse crear(ReciboRequest request) {
        var violations=validator.validate(request);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
        var venta=ventas.buscarParaActualizar(request.ventaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta no encontrada."));
        current.verificarPropietario(venta.getVendedor().getId());
        if (venta.getEstado()==EstadoVenta.ANULADA) throw new ReglaNegocioException("No se puede abonar a una venta anulada.");
        var abonado=recibos.sumarAbonos(venta.getId());
        var saldo=venta.getTotal().subtract(abonado);
        if (request.monto().compareTo(saldo)>0) throw new ReglaNegocioException("El recibo supera el saldo pendiente.");
        var recibo=new Recibo();
        recibo.setNumero("REC-"+UUID.randomUUID()); recibo.setFecha(Instant.now());
        recibo.setVenta(venta); recibo.setUsuario(current.usuario()); recibo.setMonto(request.monto());
        recibo.setObservacion(request.observacion()); recibo=recibos.save(recibo);
        // La consulta se ejecuta despues del INSERT; la fuente son los recibos.
        venta.setTotalAbonado(recibos.sumarAbonos(venta.getId()));
        venta.setSaldo(venta.getTotal().subtract(venta.getTotalAbonado()));
        venta.setEstado(venta.getSaldo().signum()==0?EstadoVenta.PAGADA:
                venta.getTotalAbonado().signum()==0?EstadoVenta.PENDIENTE:EstadoVenta.PARCIAL);
        ventas.save(venta);
        return ComercialMapper.recibo(recibo);
    }
}
