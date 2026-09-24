package com.ruth.inventio.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

/** Una unica recuperacion por usuario. Nunca contiene el token enviado por correo. */
@Getter @Setter @Entity @JsonIgnoreType
@Table(name = "password_reset_tokens")
public class PasswordResetToken extends EntidadCreada {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "token_version", nullable = false)
    private long tokenVersion;
}
