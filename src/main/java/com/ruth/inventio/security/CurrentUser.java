package com.ruth.inventio.security;

import com.ruth.inventio.entity.Usuario;
import com.ruth.inventio.model.NombreRol;
import com.ruth.inventio.model.EstadoRegistro;
import com.ruth.inventio.repository.UsuarioRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    private final UsuarioRepository usuarios;
    public CurrentUser(UsuarioRepository usuarios) { this.usuarios = usuarios; }
    public UsuarioPrincipal principal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioPrincipal principal)) {
            throw new AuthenticationCredentialsNotFoundException("Autenticacion requerida.");
        }
        return principal;
    }
    public Long id() { return principal().id(); }
    public Usuario usuario() {
        Usuario u = usuarios.findById(id()).orElseThrow(() -> new AccessDeniedException("Usuario no disponible."));
        if (u.getEstado() != EstadoRegistro.ACTIVO) throw new AccessDeniedException("Usuario inactivo.");
        return u;
    }
    public boolean soloVendedor() {
        var roles = principal().roles();
        return !roles.contains(NombreRol.ADMIN) && !roles.contains(NombreRol.SUPERVISOR);
    }
    public void verificarPropietario(Long propietarioId) {
        if (soloVendedor() && !id().equals(propietarioId)) {
            throw new AccessDeniedException("No tiene permiso para consultar esta operacion.");
        }
    }
}
