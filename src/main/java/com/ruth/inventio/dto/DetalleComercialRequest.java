package com.ruth.inventio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import com.ruth.inventio.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record DetalleComercialRequest(@NotNull @Positive Long productoId,
        @NotNull @Positive @Digits(integer=16,fraction=3) BigDecimal cantidad,
        @NotNull @Positive @Digits(integer=17,fraction=2) BigDecimal precioUnitario) {
}
