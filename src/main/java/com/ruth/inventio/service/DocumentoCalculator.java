package com.ruth.inventio.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import com.ruth.inventio.dto.DetalleComercialRequest;
import com.ruth.inventio.entity.Producto;
import com.ruth.inventio.exception.*;
import com.ruth.inventio.model.EstadoRegistro;
import com.ruth.inventio.repository.ProductoRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class DocumentoCalculator {
    public record Linea(Producto producto,BigDecimal cantidad,BigDecimal precio,BigDecimal subtotal) {}
    private final ProductoRepository productos;
    private final Validator validator;
    public DocumentoCalculator(ProductoRepository productos,Validator validator) {
        this.productos=productos; this.validator=validator;
    }
    public List<Linea> calcular(List<DetalleComercialRequest> detalles) {
        validarDetalles(detalles);
        return detalles.stream().map(d -> {
            var producto=productos.findById(d.productoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: "+d.productoId()));
            if (producto.getEstado()!=EstadoRegistro.ACTIVO) throw new ReglaNegocioException("Producto inactivo: "+producto.getCodigo());
            BigDecimal subtotal=importe(d.cantidad().multiply(d.precioUnitario()));
            if (subtotal.signum()==0) throw new ReglaNegocioException(HttpStatus.BAD_REQUEST,"El subtotal debe ser al menos 0.01.");
            return new Linea(producto,d.cantidad(),d.precioUnitario(),subtotal);
        }).toList();
    }
    public void validarDetalles(List<DetalleComercialRequest> detalles) {
        if (detalles==null || detalles.isEmpty() || detalles.size()>100) {
            throw new ReglaNegocioException(HttpStatus.BAD_REQUEST,"Debe indicar entre 1 y 100 detalles.");
        }
        detalles.forEach(d -> {
            if (d==null) throw new ReglaNegocioException(HttpStatus.BAD_REQUEST,"Detalle obligatorio.");
            var violations=validator.validate(d);
            if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
        });
    }
    public BigDecimal total(List<Linea> lineas) {
        return importe(lineas.stream().map(Linea::subtotal).reduce(BigDecimal.ZERO,BigDecimal::add));
    }
    private BigDecimal importe(BigDecimal value) {
        BigDecimal result=value.setScale(2,RoundingMode.HALF_UP);
        if (result.precision()>19) throw new ReglaNegocioException(HttpStatus.BAD_REQUEST,"Importe fuera del rango permitido.");
        return result;
    }
}
