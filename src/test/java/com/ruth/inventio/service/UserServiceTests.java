package com.ruth.inventio.service;

import java.util.Optional;
import java.util.Set;
import com.ruth.inventio.dto.*;
import com.ruth.inventio.entity.*;
import com.ruth.inventio.model.*;
import com.ruth.inventio.repository.*;
import com.ruth.inventio.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTests {
    @Test void persistsOnlyBcryptAndInvalidatesOldTokensOnUpdate() {
        var users=mock(UsuarioRepository.class); var roles=mock(RolRepository.class);
        var current=mock(CurrentUser.class); when(current.id()).thenReturn(99L);
        var encoder=new BCryptPasswordEncoder(4);
        var service=new UserService(users,roles,encoder,current);
        Rol rol=new Rol(); rol.setNombre(NombreRol.VENDEDOR);
        when(roles.findByNombre(NombreRol.VENDEDOR)).thenReturn(Optional.of(rol));
        Usuario[] persisted=new Usuario[1];
        when(users.save(any())).thenAnswer(invocation -> {
            Usuario user=invocation.getArgument(0); ReflectionTestUtils.setField(user,"id",1L);
            persisted[0]=user; return user;
        });
        var response=service.crear(new UsuarioRequest("Vendedor","VENDEDOR@example.test",
                "clave-inicial-123",Set.of(NombreRol.VENDEDOR)));
        assertEquals("vendedor@example.test",response.email());
        assertTrue(encoder.matches("clave-inicial-123",persisted[0].getPassword()));
        assertNotEquals("clave-inicial-123",persisted[0].getPassword());
        when(users.buscarParaActualizar(1L)).thenReturn(Optional.of(persisted[0]));
        service.actualizar(1L,new UsuarioUpdateRequest("Vendedor","vendedor@example.test",
                "clave-nueva-123",Set.of(NombreRol.VENDEDOR)));
        assertEquals(1,persisted[0].getTokenVersion());
        assertTrue(encoder.matches("clave-nueva-123",persisted[0].getPassword()));
        service.estado(1L,new EstadoRequest(EstadoRegistro.INACTIVO));
        assertEquals(2,persisted[0].getTokenVersion());
    }

    @Test void inventoryHttpRequestsCannotImpersonateAnotherUser() {
        var inventory=mock(InventoryService.class); var current=mock(CurrentUser.class);
        when(current.id()).thenReturn(7L);
        var service=new InventoryApiService(inventory,current);
        service.registrarEntrada(new MovimientoInventarioRequest(1L,java.math.BigDecimal.ONE,999L,null,null));
        verify(inventory).registrarEntrada(argThat(request -> request.usuarioId()==7L));
    }
}
