package com.ruth.inventio.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruth.inventio.model.TipoMovimiento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Index;
import jakarta.persistence.PreRemove;
import org.hibernate.annotations.Immutable;

@Getter
@Setter
@Entity
@Immutable
@Table(name = "movimientos_inventario",
        indexes = @Index(name = "idx_movimientos_producto_fecha", columnList = "producto_id, fecha, id"),
        check = @CheckConstraint(
        name = "ck_movimientos_inventario_valores",
        constraint = "cantidad > 0"))
public class MovimientoInventario extends EntidadCreada {

    @JsonIgnore
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimiento", nullable = false, length = 30)
    private TipoMovimiento tipoMovimiento;

    @NotNull
    @Positive
    @Digits(integer = 16, fraction = 3)
    @Column(name = "cantidad", precision = 19, scale = 3, nullable = false)
    private BigDecimal cantidad;

    @NotNull
    @Column(nullable = false)
    private Instant fecha;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Size(max = 100)
    @Column(name = "documento_origen", length = 100)
    private String documentoOrigen;

    @Size(max = 2000)
    @Column(name = "observacion", length = 2000)
    private String observacion;

    @PreRemove
    protected void impedirEliminacion() {
        throw new IllegalStateException("Los movimientos historicos se corrigen con nuevos ajustes.");
    }
}
