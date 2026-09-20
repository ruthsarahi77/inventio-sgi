package com.ruth.inventio.exception;

import java.math.BigDecimal;

public class StockInsuficienteException extends RuntimeException {
    public StockInsuficienteException(Long productoId, BigDecimal disponible, BigDecimal solicitado) {
        super("Stock insuficiente para el producto " + productoId
                + ". Disponible: " + disponible.toPlainString()
                + "; solicitado: " + solicitado.toPlainString() + ".");
    }
}
