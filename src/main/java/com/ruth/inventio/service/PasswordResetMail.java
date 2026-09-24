package com.ruth.inventio.service;

/** Transitorio: no persistir ni registrar este mensaje ni su enlace. */
public record PasswordResetMail(String recipient, String link, int ttlMinutes) {
    @Override public String toString() { return "PasswordResetMail[redacted]"; }
}
