package com.ruth.inventio.mapper;

import java.util.stream.Collectors;
import com.ruth.inventio.dto.*;
import com.ruth.inventio.entity.*;

public final class ComercialMapper {
    private ComercialMapper() {}
    public static ProductoResponse producto(Producto p) {
        return new ProductoResponse(p.getId(),p.getCodigo(),p.getNombre(),p.getDescripcion(),
                p.getPresentacion(),p.getVolumen(),p.getUnidad(),p.getCostoUnitario(),p.getEstado());
    }
    public static ClienteResponse cliente(Cliente c) {
        return new ClienteResponse(c.getId(),c.getIdentificacion(),c.getNombre(),c.getTelefono(),
                c.getEmail(),c.getDireccion(),c.getEstado());
    }
    public static UsuarioResponse usuario(Usuario u) {
        return new UsuarioResponse(u.getId(),u.getNombre(),u.getEmail(),u.getEstado(),
                u.getRoles().stream().map(Rol::getNombre).collect(Collectors.toSet()));
    }
    public static ProformaResponse proforma(Proforma p) {
        return new ProformaResponse(p.getId(),p.getNumero(),p.getFecha(),p.getCliente().getId(),
                p.getUsuario().getId(),p.getEstado(),p.getTotal(),p.getObservacion(),
                p.getDetalles().stream().map(d -> new DetalleComercialResponse(d.getId(),
                        d.getProducto().getId(),d.getCantidad(),d.getPrecioUnitario(),d.getSubtotal())).toList());
    }
    public static VentaResponse venta(Venta v) {
        return new VentaResponse(v.getId(),v.getNumero(),v.getFecha(),v.getCliente().getId(),
                v.getVendedor().getId(),v.getProforma()==null?null:v.getProforma().getId(),v.getEstado(),
                v.getTotal(),v.getTotalAbonado(),v.getSaldo(),
                v.getDetalles().stream().map(d -> new DetalleComercialResponse(d.getId(),
                        d.getProducto().getId(),d.getCantidad(),d.getPrecioUnitario(),d.getSubtotal())).toList());
    }
    public static ReciboResponse recibo(Recibo r) {
        return new ReciboResponse(r.getId(),r.getNumero(),r.getVenta().getId(),r.getFecha(),
                r.getMonto(),r.getObservacion(),r.getUsuario().getId());
    }
}
