package com.ruth.inventio.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruth.inventio.model.EstadoProforma;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.CheckConstraint;

@Getter
@Setter
@Entity
@Table(name = "proformas", check = @CheckConstraint(
        name = "ck_proformas_valores",
        constraint = "total >= 0"))
public class Proforma extends EntidadAuditable {

    @NotBlank
    @Size(max = 50)
    @Column(name = "numero", length = 50, nullable = false, unique = true)
    private String numero;

    @NotNull
    @Column(nullable = false)
    private Instant fecha;

    @JsonIgnore
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @JsonIgnore
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoProforma estado = EstadoProforma.EMITIDA;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 17, fraction = 2)
    @Column(name = "total", precision = 19, scale = 2, nullable = false)
    private BigDecimal total;

    @Size(max = 2000)
    @Column(name = "observacion", length = 2000)
    private String observacion;

    @JsonIgnore
    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "proforma", fetch = FetchType.LAZY)
    private List<ProformaDetalle> detalles = new ArrayList<>();
}
