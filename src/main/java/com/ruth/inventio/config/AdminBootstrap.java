package com.ruth.inventio.config;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import com.ruth.inventio.dto.UsuarioRequest;
import com.ruth.inventio.entity.Rol;
import com.ruth.inventio.entity.Usuario;
import com.ruth.inventio.model.NombreRol;
import com.ruth.inventio.model.EstadoRegistro;
import com.ruth.inventio.repository.RolRepository;
import com.ruth.inventio.repository.UsuarioRepository;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Provision inicial optativa. Nunca modifica usuarios existentes ni imprime credenciales. */
@Component
public class AdminBootstrap implements ApplicationRunner {
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final PasswordEncoder passwords;
    private final Validator validator;
    private final String email;
    private final String name;
    private final String password;
    public AdminBootstrap(UsuarioRepository usuarios,RolRepository roles,PasswordEncoder passwords,Validator validator,
            @Value("#{systemEnvironment['INITIAL_ADMIN_NAME'] ?: ''}") String name,
            @Value("#{systemEnvironment['INITIAL_ADMIN_EMAIL'] ?: ''}") String email,
            @Value("#{systemEnvironment['INITIAL_ADMIN_PASSWORD'] ?: ''}") String password) {
        this.usuarios=usuarios; this.roles=roles; this.passwords=passwords;
        this.validator=validator; this.name=name; this.email=email; this.password=password;
    }
    @Override @Transactional
    public void run(ApplicationArguments args) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) return;
        String normalizedEmail=email.trim().toLowerCase(Locale.ROOT);
        if (usuarios.findByEmailIgnoreCase(normalizedEmail).isPresent()) return;
        var request=new UsuarioRequest(name.trim(),normalizedEmail,password,Set.of(NombreRol.ADMIN));
        if (!validator.validate(request).isEmpty() || password.getBytes(StandardCharsets.UTF_8).length>72) {
            throw new IllegalStateException("Configurar INITIAL_ADMIN_NAME, INITIAL_ADMIN_EMAIL y INITIAL_ADMIN_PASSWORD validos (clave de 12-72 caracteres, maximo 72 bytes).");
        }
        for (NombreRol nombre:NombreRol.values()) {
            if (roles.findByNombre(nombre).isEmpty()) {
                Rol rol=new Rol(); rol.setNombre(nombre); roles.save(rol);
            }
        }
        Usuario usuario=new Usuario();
        usuario.setNombre(name.trim()); usuario.setEmail(normalizedEmail);
        usuario.setEstado(EstadoRegistro.ACTIVO);
        usuario.setPassword(passwords.encode(password));
        usuario.getRoles().add(roles.findByNombre(NombreRol.ADMIN).orElseThrow());
        usuarios.save(usuario);
    }
}
