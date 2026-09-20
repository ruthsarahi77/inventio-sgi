package com.ruth.inventio.entity;

import com.ruth.inventio.model.NombreRol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "roles")
public class Rol extends EntidadAuditable {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "nombre", nullable = false, length = 30, unique = true)
    private NombreRol nombre;
}
