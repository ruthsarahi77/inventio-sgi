package com.ruth.inventio.controller;

import com.ruth.inventio.dto.ForgotPasswordRequest;
import com.ruth.inventio.dto.MessageResponse;
import com.ruth.inventio.dto.ResetPasswordRequest;
import com.ruth.inventio.exception.ReglaNegocioException;
import com.ruth.inventio.security.RecoveryRateLimiter;
import com.ruth.inventio.service.PasswordRecoveryDispatcher;
import com.ruth.inventio.service.PasswordRecoveryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class PasswordRecoveryController {
    private final PasswordRecoveryDispatcher dispatcher;
    private final PasswordRecoveryService service;
    private final RecoveryRateLimiter limiter;
    public PasswordRecoveryController(PasswordRecoveryDispatcher dispatcher, PasswordRecoveryService service,
            RecoveryRateLimiter limiter) { this.dispatcher=dispatcher; this.service=service; this.limiter=limiter; }

    @PostMapping("/forgot-password")
    public MessageResponse forgot(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest http) {
        // No confiar en X-Forwarded-For enviado directamente por el cliente.
        if (limiter.allowForgot(http.getRemoteAddr(),request.email())) dispatcher.submit(request.email());
        return new MessageResponse("Si el correo está registrado, recibirás instrucciones para restablecer tu contraseña.");
    }

    @PostMapping("/reset-password")
    public MessageResponse reset(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest http) {
        if (!limiter.allowReset(http.getRemoteAddr())) {
            throw new ReglaNegocioException(HttpStatus.TOO_MANY_REQUESTS,"Demasiados intentos. Inténtalo más tarde.");
        }
        service.reset(request.token(),request.newPassword());
        return new MessageResponse("Contraseña restablecida. Inicia sesión con tu nueva contraseña.");
    }
}
