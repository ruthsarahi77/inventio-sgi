package com.ruth.inventio.repository;

import java.math.BigDecimal;

/** Resultado agregado de una consulta, sin cargar entidades ni colecciones. */
public interface StockProductoProjection {
    Long getIdProducto();
    String getCodigo();
    String getNombre();
    String getPresentacion();
    String getUnidad();
    BigDecimal getCostoUnitario();
    BigDecimal getStockActual();
}
