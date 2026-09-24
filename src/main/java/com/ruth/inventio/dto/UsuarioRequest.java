package com.ruth.inventio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import com.ruth.inventio.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;

public record UsuarioRequest(@NotBlank @Size(max=200) String nombre, @NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(min=com.ruth.inventio.security.PasswordPolicy.MIN_LENGTH,
                max=com.ruth.inventio.security.PasswordPolicy.MAX_LENGTH) String password,
        @JsonAlias("rol") @JsonFormat(with=JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        @NotEmpty Set<@NotNull NombreRol> roles, EstadoRegistro estado) {
    public UsuarioRequest {
        if (estado == null) estado = EstadoRegistro.ACTIVO;
    }
    public UsuarioRequest(String nombre, String email, String password, Set<NombreRol> roles) {
        this(nombre, email, password, roles, EstadoRegistro.ACTIVO);
    }
}
