package com.ruth.inventio.config;

import com.ruth.inventio.entity.Rol;
import com.ruth.inventio.entity.Usuario;
import com.ruth.inventio.model.EstadoRegistro;
import com.ruth.inventio.model.NombreRol;
import com.ruth.inventio.repository.RolRepository;
import com.ruth.inventio.repository.UsuarioRepository;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminBootstrapTests {
    private static ValidatorFactory validation;
    private final UsuarioRepository usuarios=mock(UsuarioRepository.class);
    private final RolRepository roles=mock(RolRepository.class);
    private final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder(4);
    private static final String PASSWORD="Prueba-inicial-123";

    @BeforeAll static void setup() { validation=Validation.buildDefaultValidatorFactory(); }
    @AfterAll static void close() { validation.close(); }
    private AdminBootstrap bootstrap(String name,String email,String password) {
        return new AdminBootstrap(usuarios,roles,encoder,validation.getValidator(),name,email,password);
    }

    @Test void createsActiveAdminWithBcryptEvenWhenOtherUsersExistAndDoesNotDuplicateOnRestart() {
        var persisted=new AtomicReference<Usuario>();
        when(usuarios.count()).thenReturn(5L);
        when(usuarios.findByEmailIgnoreCase("admin@example.test"))
                .thenAnswer(invocation -> Optional.ofNullable(persisted.get()));
        when(usuarios.save(any())).thenAnswer(invocation -> {
            Usuario usuario=invocation.getArgument(0); persisted.set(usuario); return usuario;
        });
        for (NombreRol nombre:NombreRol.values()) {
            Rol rol=new Rol(); rol.setNombre(nombre);
            when(roles.findByNombre(nombre)).thenReturn(Optional.empty(),Optional.of(rol));
        }
        bootstrap(" Administrador "," ADMIN@example.test ",PASSWORD).run(null);
        Usuario created=persisted.get();
        assertNotNull(created);
        assertEquals("Administrador",created.getNombre());
        assertEquals("admin@example.test",created.getEmail());
        assertEquals(EstadoRegistro.ACTIVO,created.getEstado());
        assertEquals(1,created.getRoles().size());
        assertEquals(NombreRol.ADMIN,created.getRoles().iterator().next().getNombre());
        assertNotEquals(PASSWORD,created.getPassword());
        assertTrue(encoder.matches(PASSWORD,created.getPassword()));
        String hash=created.getPassword();
        bootstrap("Otro nombre","admin@example.test","Otra-clave-456").run(null);
        verify(usuarios,times(1)).save(any());
        assertEquals(hash,created.getPassword());
        assertEquals("Administrador",created.getNombre());
    }

    @Test void existingInactiveSellerIsNeverChangedOrPromoted() {
        Usuario existing=new Usuario(); existing.setNombre("Vendedor");
        existing.setEstado(EstadoRegistro.INACTIVO); existing.setPassword("existing-hash");
        Rol rol=new Rol(); rol.setNombre(NombreRol.VENDEDOR); existing.getRoles().add(rol);
        when(usuarios.findByEmailIgnoreCase("admin@example.test")).thenReturn(Optional.of(existing));
        bootstrap("Administrador","admin@example.test",PASSWORD).run(null);
        verify(usuarios,never()).save(any()); verifyNoInteractions(roles);
        assertEquals(EstadoRegistro.INACTIVO,existing.getEstado());
        assertEquals("existing-hash",existing.getPassword());
        assertEquals(NombreRol.VENDEDOR,existing.getRoles().iterator().next().getNombre());
    }

    @ParameterizedTest
    @CsvSource(value={"'',admin@example.test,Prueba-inicial-123", "Administrador,'',Prueba-inicial-123",
            "Administrador,admin@example.test,''", "' ',' ',' '"})
    void missingVariablesSkipCreation(String name,String email,String password) {
        assertDoesNotThrow(() -> bootstrap(name,email,password).run(null));
        verifyNoInteractions(usuarios,roles);
    }

    @Test void invalidConfigurationFailsWithoutDisclosingPassword() {
        var ex=assertThrows(IllegalStateException.class,
                () -> bootstrap("Administrador","invalid-email",PASSWORD).run(null));
        assertFalse(ex.getMessage().contains(PASSWORD));
        verify(usuarios,never()).save(any());
        assertThrows(IllegalStateException.class,
                () -> bootstrap("Administrador","admin@example.test","é".repeat(40)).run(null));
    }
}
