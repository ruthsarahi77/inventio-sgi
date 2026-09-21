package com.ruth.inventio.dto;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonInclude;

/** null indica información no determinable con el modelo/configuración actual. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record DashboardStatsResponse(BigDecimal litrosVendidosMes, BigDecimal metaRepsol,
        BigDecimal cumplimientoMeta, BigDecimal totalVentasMes, BigDecimal totalVentasUSD,
        BigDecimal saldoPendienteTotal, Long productosStockBajo, Long totalProductos,
        Long ventasHoy, Long recibosHoy) {}
