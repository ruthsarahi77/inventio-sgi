package com.ruth.inventio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import com.ruth.inventio.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record ProformaRequest(@NotNull @Positive Long clienteId,
        @NotEmpty @Size(max=100) List<@NotNull @Valid DetalleComercialRequest> detalles,
        @Size(max=2000) String observacion) {
}
