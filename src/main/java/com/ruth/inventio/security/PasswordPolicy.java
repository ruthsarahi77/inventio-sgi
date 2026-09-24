package com.ruth.inventio.security;

import com.ruth.inventio.exception.ReglaNegocioException;
import org.springframework.http.HttpStatus;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/** Politica comun para nuevas contrasenas; no se aplica al login de cuentas existentes. */
public final class PasswordPolicy {
    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 72;
    public static final String MESSAGE = "La contrasena debe tener 12-72 caracteres, hasta 72 bytes UTF-8, y no ser trivial ni un hash.";
    private static final Set<String> COMMON = Set.of("123456789012", "1234567890123456", "password1234",
            "password12345", "password123!", "qwertyuiop123", "administrador", "inventio12345");
    private PasswordPolicy() {}

    public static boolean isValid(String password) {
        if (password == null) return false;
        String meaningful = password.strip();
        return meaningful.length() >= MIN_LENGTH && password.length() <= MAX_LENGTH
                && password.getBytes(StandardCharsets.UTF_8).length <= MAX_LENGTH
                && meaningful.codePoints().distinct().limit(2).count() == 2
                && !COMMON.contains(meaningful.toLowerCase(Locale.ROOT))
                && !password.matches("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");
    }

    public static void validate(String password) {
        if (!isValid(password)) throw new ReglaNegocioException(HttpStatus.BAD_REQUEST, MESSAGE);
    }
}
