package com.ruth.inventio.exception;

import java.time.Instant;

import com.ruth.inventio.dto.ApiError;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Object> handleBusiness(ReglaNegocioException ex, WebRequest request) {
        return ResponseEntity.status(ex.getStatus()).body(error(ex.getStatus(), ex.getMessage(), request));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleIntegrity(DataIntegrityViolationException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error(HttpStatus.CONFLICT,
                "Conflicto de datos: identificador duplicado o relacion no valida.", request));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleForbidden(AccessDeniedException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error(HttpStatus.FORBIDDEN,
                "Operacion no autorizada.", request));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthentication(AuthenticationException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error(HttpStatus.UNAUTHORIZED,
                "Credenciales invalidas o autenticacion requerida.", request));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Object> handleNotFound(RecursoNoEncontradoException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error(HttpStatus.NOT_FOUND, ex.getMessage(), request));
    }

    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<Object> handleInsufficientStock(StockInsuficienteException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(HttpStatus.CONFLICT, ex.getMessage(), request));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleInvalidData(ConstraintViolationException ex, WebRequest request) {
        return ResponseEntity.badRequest().body(error(HttpStatus.BAD_REQUEST,
                "Datos invalidos. Revise identificadores, cantidad y longitudes permitidas.", request));
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<Object> handleLockConflict(PessimisticLockingFailureException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error(HttpStatus.CONFLICT,
                "No fue posible obtener el bloqueo del producto. Reintente la operacion.", request));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = status.is5xxServerError()
                ? "Ocurrio un error interno." : "La solicitud no pudo procesarse. Revise los datos enviados.";
        return new ResponseEntity<>(error(status, message, request), headers, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpectedException(Exception ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrio un error interno.", request));
    }

    private ApiError error(HttpStatusCode status, String message, WebRequest request) {
        HttpStatus knownStatus = HttpStatus.resolve(status.value());
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        return new ApiError(Instant.now(), status.value(),
                knownStatus == null ? "HTTP Error" : knownStatus.getReasonPhrase(), message, path);
    }
}
