package com.ruth.inventio.exception;

public class CodigoDuplicadoException extends ReglaNegocioException {
    public CodigoDuplicadoException(String codigo) { super("Codigo de producto duplicado: " + codigo); }
}
