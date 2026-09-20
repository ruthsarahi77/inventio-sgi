package com.ruth.inventio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import com.ruth.inventio.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record ProductoRequest(@NotBlank @Size(max=100) String codigo, @NotBlank @Size(max=200) String nombre,
        @Size(max=2000) String descripcion, @Size(max=100) String presentacion,
        @Positive @Digits(integer=16,fraction=3) BigDecimal volumen, @Size(max=30) String unidad,
        @NotNull @PositiveOrZero @Digits(integer=17,fraction=2) BigDecimal costoUnitario) {
}
