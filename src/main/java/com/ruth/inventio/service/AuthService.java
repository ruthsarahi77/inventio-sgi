package com.ruth.inventio.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import com.ruth.inventio.dto.LoginRequest;
import com.ruth.inventio.dto.LoginResponse;
import com.ruth.inventio.mapper.ComercialMapper;
import com.ruth.inventio.model.EstadoRegistro;
import com.ruth.inventio.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final String issuer;
    private final String audience;
    private final long ttl;
    private final String dummyHash;
    public AuthService(UsuarioRepository usuarios, PasswordEncoder passwords, JwtEncoder encoder,
            @Value("${inventio.jwt.issuer}") String issuer, @Value("${inventio.jwt.audience}") String audience,
            @Value("${inventio.jwt.ttl-seconds}") long ttl) {
        if (ttl < 60 || ttl > 3600) throw new IllegalStateException("JWT_TTL_SECONDS debe estar entre 60 y 3600.");
        this.usuarios=usuarios; this.passwords=passwords; this.encoder=encoder;
        this.issuer=issuer; this.audience=audience; this.ttl=ttl;
        this.dummyHash=passwords.encode(java.util.UUID.randomUUID().toString());
    }
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        var usuario=usuarios.findByEmailIgnoreCase(request.email().trim().toLowerCase(Locale.ROOT)).orElse(null);
        boolean matches=passwords.matches(request.password(),usuario==null?dummyHash:usuario.getPassword());
        if (!matches || usuario==null || usuario.getEstado()!=EstadoRegistro.ACTIVO || usuario.getRoles().isEmpty()) {
            throw new BadCredentialsException("Credenciales invalidas.");
        }
        Instant now=Instant.now();
        var claims=JwtClaimsSet.builder().issuer(issuer).audience(List.of(audience))
                .subject(usuario.getId().toString()).issuedAt(now).expiresAt(now.plusSeconds(ttl))
                .id(java.util.UUID.randomUUID().toString()).claim("ver",usuario.getTokenVersion())
                .claim("roles",usuario.getRoles().stream().map(r -> r.getNombre().name()).toList()).build();
        var token=encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        return new LoginResponse(token,"Bearer",ttl,ComercialMapper.usuario(usuario));
    }
}
