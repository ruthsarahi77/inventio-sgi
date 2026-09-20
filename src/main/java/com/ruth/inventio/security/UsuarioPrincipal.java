package com.ruth.inventio.security;

import java.security.Principal;
import java.util.Set;
import com.ruth.inventio.model.NombreRol;

public record UsuarioPrincipal(Long id, Set<NombreRol> roles) implements Principal {
    public UsuarioPrincipal { roles = Set.copyOf(roles); }
    @Override public String getName() { return id.toString(); }
}
