package com.ruth.inventio.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.CheckConstraint;

@Getter
@Setter
@Entity
@Table(name = "recibos", check = @CheckConstraint(
        name = "ck_recibos_valores",
        constraint = "monto > 0"))
public class Recibo extends EntidadCreada {

    @NotBlank
    @Size(max = 50)
    @Column(name = "numero", length = 50, nullable = false, unique = true)
    private String numero;

    @JsonIgnore
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @NotNull
    @Column(nullable = false)
    private Instant fecha;

    @NotNull
    @Positive
    @Digits(integer = 17, fraction = 2)
    @Column(name = "monto", precision = 19, scale = 2, nullable = false)
    private BigDecimal monto;

    @Size(max = 2000)
    @Column(name = "observacion", length = 2000)
    private String observacion;

    @JsonIgnore
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
}
