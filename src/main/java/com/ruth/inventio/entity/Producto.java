package com.ruth.inventio.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruth.inventio.model.EstadoRegistro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.CheckConstraint;

@Getter
@Setter
@Entity
@Table(name = "productos", check = @CheckConstraint(
        name = "ck_productos_valores",
        constraint = "costo_unitario >= 0 and (volumen is null or volumen > 0)"))
public class Producto extends EntidadAuditable {

    @NotBlank
    @Size(max = 100)
    @Column(name = "codigo", length = 100, nullable = false, unique = true)
    private String codigo;

    @NotBlank
    @Size(max = 200)
    @Column(name = "nombre", length = 200, nullable = false)
    private String nombre;

    @Size(max = 2000)
    @Column(name = "descripcion", length = 2000)
    private String descripcion;

    @Size(max = 100)
    @Column(name = "presentacion", length = 100)
    private String presentacion;

    @Positive
    @Digits(integer = 16, fraction = 3)
    @Column(name = "volumen", precision = 19, scale = 3)
    private BigDecimal volumen;

    @Size(max = 30)
    @Column(name = "unidad", length = 30)
    private String unidad;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 17, fraction = 2)
    @Column(name = "costo_unitario", precision = 19, scale = 2, nullable = false)
    private BigDecimal costoUnitario;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoRegistro estado = EstadoRegistro.ACTIVO;

    @JsonIgnore
    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "producto", fetch = FetchType.LAZY)
    private List<MovimientoInventario> movimientos = new ArrayList<>();

    @JsonIgnore
    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "producto", fetch = FetchType.LAZY)
    private List<ProformaDetalle> proformaDetalles = new ArrayList<>();

    @JsonIgnore
    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "producto", fetch = FetchType.LAZY)
    private List<VentaDetalle> ventaDetalles = new ArrayList<>();
}
