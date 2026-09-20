package com.ruth.inventio.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MovimientoInventarioRequest(
        @NotNull @Positive Long productoId,
        @NotNull @Positive @Digits(integer = 16, fraction = 3) BigDecimal cantidad,
        @Positive Long usuarioId,
        @Size(max = 100) String documentoOrigen,
        @Size(max = 2000) String observacion) {
}
