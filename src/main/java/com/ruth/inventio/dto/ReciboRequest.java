package com.ruth.inventio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import com.ruth.inventio.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record ReciboRequest(@NotNull @Positive Long ventaId,
        @NotNull @Positive @Digits(integer=17,fraction=2) BigDecimal monto, @Size(max=2000) String observacion) {
}
