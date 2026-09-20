package com.ruth.inventio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import com.ruth.inventio.model.TipoMovimiento;

public record MovimientoInventarioResponse(Long idMovimiento, Long idProducto,
        TipoMovimiento tipoMovimiento, BigDecimal cantidad, Instant fecha,
        UsuarioInventarioResponse usuario, String documentoOrigen, String observacion,
        BigDecimal saldoAcumulado) {
}
