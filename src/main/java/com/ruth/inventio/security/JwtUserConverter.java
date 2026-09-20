package com.ruth.inventio.security;

import java.util.stream.Collectors;
import com.ruth.inventio.entity.Rol;
import com.ruth.inventio.model.EstadoRegistro;
import com.ruth.inventio.repository.UsuarioRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JwtUserConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final UsuarioRepository usuarios;
    public JwtUserConverter(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    @Override
    @Transactional(readOnly = true)
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long id;
        try { id = Long.valueOf(jwt.getSubject()); }
        catch (RuntimeException ex) { throw new InvalidBearerTokenException("Token invalido."); }
        var usuario = usuarios.findById(id).orElseThrow(() -> new InvalidBearerTokenException("Token invalido."));
        Object version = jwt.getClaim("ver");
        if (usuario.getEstado() != EstadoRegistro.ACTIVO || !(version instanceof Number number)
                || number.longValue() != usuario.getTokenVersion() || usuario.getRoles().isEmpty()) {
            throw new InvalidBearerTokenException("Token revocado o usuario inactivo.");
        }
        var roles = usuario.getRoles().stream().map(Rol::getNombre).collect(Collectors.toSet());
        var authorities = roles.stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r.name())).toList();
        return new UsernamePasswordAuthenticationToken(new UsuarioPrincipal(id,roles), null, authorities);
    }
}
