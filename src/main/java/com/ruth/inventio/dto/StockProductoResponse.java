package com.ruth.inventio.dto;

import java.math.BigDecimal;

public record StockProductoResponse(Long idProducto, String codigo, String nombre,
        String presentacion, String unidad, BigDecimal costoUnitario,
        BigDecimal stockActual, BigDecimal valorInventario) {
}
