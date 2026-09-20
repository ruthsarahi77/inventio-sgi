package com.ruth.inventio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import com.ruth.inventio.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record VentaResponse(Long id, String numero, Instant fecha, Long clienteId, Long vendedorId, Long proformaId,
        EstadoVenta estado, BigDecimal total, BigDecimal totalAbonado, BigDecimal saldo,
        List<DetalleComercialResponse> detalles) {
}
