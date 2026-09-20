package com.ruth.inventio.dto;

import java.math.BigDecimal;
import java.util.List;

public record KardexResponse(Long idProducto, String codigo, String nombre,
        BigDecimal stockActual, List<MovimientoInventarioResponse> movimientos) {
    public KardexResponse {
        movimientos = List.copyOf(movimientos);
    }
}
