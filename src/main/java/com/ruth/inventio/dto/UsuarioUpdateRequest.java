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

public record UsuarioUpdateRequest(@NotBlank @Size(max=200) String nombre, @NotBlank @Email @Size(max=254) String email,
        @com.ruth.inventio.validation.ValidPassword(optional=true) String password,
        @JsonAlias("rol") @JsonFormat(with=JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        @NotEmpty Set<@NotNull NombreRol> roles) {
}
