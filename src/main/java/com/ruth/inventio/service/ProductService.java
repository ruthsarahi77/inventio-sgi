package com.ruth.inventio.service;

import java.util.List;
import com.ruth.inventio.dto.*;
import com.ruth.inventio.entity.Producto;
import com.ruth.inventio.exception.*;
import com.ruth.inventio.mapper.ComercialMapper;
import com.ruth.inventio.repository.ProductoRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class ProductService {
    private final ProductoRepository productos;
    public ProductService(ProductoRepository productos) { this.productos=productos; }
    public List<ProductoResponse> listar() { return productos.findAll().stream().map(ComercialMapper::producto).toList(); }
    public ProductoResponse obtener(Long id) { return ComercialMapper.producto(productos.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: "+id))); }
    @Transactional @PreAuthorize("hasRole('ADMIN')")
    public ProductoResponse crear(ProductoRequest r) {
        comprobarCodigo(r.codigo().trim(),null);
        return ComercialMapper.producto(productos.save(aplicar(new Producto(),r)));
    }
    @Transactional @PreAuthorize("hasRole('ADMIN')")
    public ProductoResponse actualizar(Long id,ProductoRequest r) {
        var p=productos.buscarParaActualizar(id).orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: "+id));
        comprobarCodigo(r.codigo().trim(),id);
        return ComercialMapper.producto(productos.save(aplicar(p,r)));
    }
    @Transactional @PreAuthorize("hasRole('ADMIN')")
    public ProductoResponse estado(Long id,EstadoRequest r) {
        var p=productos.buscarParaActualizar(id).orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: "+id));
        p.setEstado(r.estado());
        return ComercialMapper.producto(productos.save(p));
    }
    private void comprobarCodigo(String codigo,Long id) {
        productos.findByCodigo(codigo).filter(p -> !p.getId().equals(id))
                .ifPresent(p -> { throw new CodigoDuplicadoException(codigo); });
    }
    private Producto aplicar(Producto p,ProductoRequest r) {
        p.setCodigo(r.codigo().trim()); p.setNombre(r.nombre().trim()); p.setDescripcion(r.descripcion());
        p.setPresentacion(r.presentacion()); p.setVolumen(r.volumen()); p.setUnidad(r.unidad());
        p.setCostoUnitario(r.costoUnitario()); return p;
    }
}
