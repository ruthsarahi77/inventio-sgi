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
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "clientes")
public class Cliente extends EntidadAuditable {

    @NotBlank
    @Size(max = 50)
    @Column(name = "identificacion", length = 50, nullable = false, unique = true)
    private String identificacion;

    @NotBlank
    @Size(max = 200)
    @Column(name = "nombre", length = 200, nullable = false)
    private String nombre;

    @Size(max = 30)
    @Column(name = "telefono", length = 30)
    private String telefono;

    @Email
    @Size(max = 254)
    @Column(name = "email", length = 254)
    private String email;

    @Size(max = 500)
    @Column(name = "direccion", length = 500)
    private String direccion;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoRegistro estado = EstadoRegistro.ACTIVO;

    @JsonIgnore
    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "cliente", fetch = FetchType.LAZY)
    private List<Proforma> proformas = new ArrayList<>();

    @JsonIgnore
    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "cliente", fetch = FetchType.LAZY)
    private List<Venta> ventas = new ArrayList<>();
}
