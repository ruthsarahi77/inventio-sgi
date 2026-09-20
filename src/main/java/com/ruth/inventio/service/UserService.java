package com.ruth.inventio.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import com.ruth.inventio.dto.*;
import com.ruth.inventio.entity.Usuario;
import com.ruth.inventio.exception.*;
import com.ruth.inventio.mapper.ComercialMapper;
import com.ruth.inventio.model.*;
import com.ruth.inventio.repository.*;
import com.ruth.inventio.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly=true)
public class UserService {
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder passwords;
    private final CurrentUser current;
    public UserService(UsuarioRepository usuarios,RolRepository roles,PasswordEncoder passwords,CurrentUser current) {
        this.usuarios=usuarios; this.roles=roles; this.passwords=passwords; this.current=current;
    }
    public List<UsuarioResponse> listar() { return usuarios.findAll().stream().map(ComercialMapper::usuario).toList(); }
    @Transactional
    public UsuarioResponse crear(UsuarioRequest r) {
        var u=new Usuario(); actualizarDatos(u,r.nombre(),r.email(),r.password(),r.roles());
        return ComercialMapper.usuario(usuarios.save(u));
    }
    @Transactional
    public UsuarioResponse actualizar(Long id,UsuarioUpdateRequest r) {
        var u=buscar(id);
        if (id.equals(current.id()) && !r.roles().contains(NombreRol.ADMIN)) {
            throw new ReglaNegocioException("No puede retirar su propio rol ADMIN.");
        }
        actualizarDatos(u,r.nombre(),r.email(),r.password(),r.roles());
        u.setTokenVersion(u.getTokenVersion()+1);
        return ComercialMapper.usuario(usuarios.save(u));
    }
    @Transactional
    public UsuarioResponse estado(Long id,EstadoRequest r) {
        var u=buscar(id);
        if (id.equals(current.id()) && r.estado()==EstadoRegistro.INACTIVO) {
            throw new ReglaNegocioException("No puede desactivar su propia cuenta.");
        }
        u.setEstado(r.estado()); u.setTokenVersion(u.getTokenVersion()+1);
        return ComercialMapper.usuario(usuarios.save(u));
    }
    private Usuario buscar(Long id) { return usuarios.buscarParaActualizar(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: "+id)); }
    private void actualizarDatos(Usuario u,String nombre,String email,String password,Set<NombreRol> nombres) {
        String normalizado=email.trim().toLowerCase(Locale.ROOT);
        usuarios.findByEmailIgnoreCase(normalizado).filter(e -> !e.getId().equals(u.getId()))
                .ifPresent(e -> { throw new ReglaNegocioException("Email de usuario duplicado."); });
        u.setNombre(nombre.trim()); u.setEmail(normalizado);
        if (password!=null) {
            if (password.getBytes(StandardCharsets.UTF_8).length>72) {
                throw new ReglaNegocioException(HttpStatus.BAD_REQUEST,"La contrasena supera 72 bytes UTF-8.");
            }
            u.setPassword(passwords.encode(password));
        }
        u.getRoles().clear();
        for (NombreRol nombreRol:nombres) {
            u.getRoles().add(roles.findByNombre(nombreRol)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Rol no registrado: "+nombreRol)));
        }
    }
}
