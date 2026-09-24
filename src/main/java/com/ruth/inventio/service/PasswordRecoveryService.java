package com.ruth.inventio.service;

import com.ruth.inventio.config.PasswordRecoveryProperties;
import com.ruth.inventio.entity.PasswordResetToken;
import com.ruth.inventio.exception.ReglaNegocioException;
import com.ruth.inventio.model.EstadoRegistro;
import com.ruth.inventio.repository.PasswordResetTokenRepository;
import com.ruth.inventio.repository.UsuarioRepository;
import com.ruth.inventio.security.PasswordPolicy;
import com.ruth.inventio.security.RecoveryTokens;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.Locale;
import java.util.Optional;

@Service
public class PasswordRecoveryService {
    private final UsuarioRepository usuarios;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwords;
    private final PasswordRecoveryProperties properties;
    private final Clock clock;
    public PasswordRecoveryService(UsuarioRepository usuarios, PasswordResetTokenRepository tokens,
            PasswordEncoder passwords, PasswordRecoveryProperties properties,
            @Qualifier("passwordRecoveryClock") Clock clock) {
        this.usuarios=usuarios; this.tokens=tokens; this.passwords=passwords; this.properties=properties; this.clock=clock;
    }

    /** El llamador envia el correo solo despues de que este metodo confirme su transaccion. */
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Optional<PasswordResetMail> issue(String email) {
        if (!properties.configured()) return Optional.empty();
        String normalized=email.trim().toLowerCase(Locale.ROOT);
        // Proyeccion escalar: evita cargar una entidad obsoleta antes de adquirir el bloqueo.
        var id=usuarios.findIdByEmailIgnoreCase(normalized);
        if (id.isEmpty()) return Optional.empty();
        var usuario=usuarios.buscarParaActualizar(id.get()).orElse(null);
        if (usuario == null || usuario.getEstado()!=EstadoRegistro.ACTIVO || !usuario.getEmail().equalsIgnoreCase(normalized)) {
            return Optional.empty();
        }
        var reset=tokens.findByUsuarioId(usuario.getId()).orElseGet(PasswordResetToken::new);
        String raw=RecoveryTokens.generate();
        reset.setUsuario(usuario); reset.setTokenHash(RecoveryTokens.hash(raw));
        reset.setExpiresAt(clock.instant().plusSeconds(properties.ttlMinutes()*60L));
        reset.setUsedAt(null); reset.setTokenVersion(usuario.getTokenVersion());
        tokens.save(reset);
        return Optional.of(new PasswordResetMail(usuario.getEmail(),properties.link(raw),properties.ttlMinutes()));
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    public void reset(String token, String newPassword) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw invalidToken();
        PasswordPolicy.validate(newPassword);
        Long id=tokens.findUsuarioIdByTokenHash(RecoveryTokens.hash(token)).orElseThrow(PasswordRecoveryService::invalidToken);
        // Mismo orden de bloqueo que issue y gestion de usuarios. Serializa usos simultaneos.
        var usuario=usuarios.buscarParaActualizar(id).orElseThrow(PasswordRecoveryService::invalidToken);
        var reset=tokens.findByUsuarioId(id).orElseThrow(PasswordRecoveryService::invalidToken);
        if (!RecoveryTokens.matches(token,reset.getTokenHash()) || reset.getUsedAt()!=null
                || !reset.getExpiresAt().isAfter(clock.instant()) || usuario.getEstado()!=EstadoRegistro.ACTIVO
                || reset.getTokenVersion()!=usuario.getTokenVersion()) throw invalidToken();
        usuario.setPassword(passwords.encode(newPassword));
        usuario.setTokenVersion(usuario.getTokenVersion()+1);
        reset.setUsedAt(clock.instant());
        usuarios.save(usuario); tokens.save(reset);
    }

    private static ReglaNegocioException invalidToken() {
        return new ReglaNegocioException(HttpStatus.BAD_REQUEST,"El enlace de recuperación es inválido o ha expirado.");
    }
}
