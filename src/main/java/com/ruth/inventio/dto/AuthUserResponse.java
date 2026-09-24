package com.ruth.inventio.dto;

import com.ruth.inventio.model.NombreRol;

/** Datos publicos de la cuenta. Los permisos se resuelven siempre en el backend. */
public record AuthUserResponse(Long id, String nombre, String email, NombreRol rol) {
    public static AuthUserResponse from(UsuarioResponse usuario) {
        // Conserva las cuentas multirrol existentes y su precedencia de permisos.
        NombreRol rol = usuario.roles().contains(NombreRol.ADMIN) ? NombreRol.ADMIN
                : usuario.roles().contains(NombreRol.SUPERVISOR) ? NombreRol.SUPERVISOR : NombreRol.VENDEDOR;
        return new AuthUserResponse(usuario.id(), usuario.nombre(), usuario.email(), rol);
    }
}
