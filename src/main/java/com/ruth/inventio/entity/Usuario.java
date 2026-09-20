package com.ruth.inventio.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruth.inventio.model.EstadoRegistro;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "usuarios")
public class Usuario extends EntidadAuditable {

    @NotBlank
    @Size(max = 200)
    @Column(name = "nombre", length = 200, nullable = false)
    private String nombre;

    @NotBlank
    @Email
    @Size(max = 254)
    @Column(name = "email", length = 254, nullable = false, unique = true)
    private String email;

    // Hash BCrypt, nunca una contrasena en texto plano.
    @JsonIgnore
    @NotBlank
    @Size(max = 255)
    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @JsonIgnore
    @Column(name = "token_version", nullable = false)
    private long tokenVersion;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoRegistro estado = EstadoRegistro.ACTIVO;

    @JsonIgnore
    @NotEmpty
    @Setter(AccessLevel.NONE)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "usuarios_roles",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id"))
    private Set<Rol> roles = new LinkedHashSet<>();

    @JsonIgnore
    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "vendedor", fetch = FetchType.LAZY)
    private List<Venta> ventas = new ArrayList<>();

    @JsonIgnore
    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    private List<Proforma> proformas = new ArrayList<>();

    @JsonIgnore
    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    private List<MovimientoInventario> movimientos = new ArrayList<>();

    @JsonIgnore
    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    private List<Recibo> recibos = new ArrayList<>();
}
