package com.ruth.inventio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import com.ruth.inventio.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record VentaRequest(@Positive Long clienteId, @Positive Long proformaId,
        @Size(min=1,max=100) List<@NotNull @Valid DetalleComercialRequest> detalles) {
}
