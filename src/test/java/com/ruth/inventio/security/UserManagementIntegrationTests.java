package com.ruth.inventio.security;

import com.ruth.inventio.controller.AuthController;
import com.ruth.inventio.controller.UserController;
import com.ruth.inventio.controller.PasswordRecoveryController;
import com.ruth.inventio.config.PasswordRecoveryProperties;
import com.ruth.inventio.entity.PasswordResetToken;
import com.ruth.inventio.repository.PasswordResetTokenRepository;
import com.ruth.inventio.service.PasswordRecoveryService;
import com.ruth.inventio.service.PasswordRecoveryDispatcher;
import com.ruth.inventio.service.PasswordResetMailer;
import com.ruth.inventio.service.PasswordResetMail;
import org.springframework.core.task.TaskExecutor;
import org.mockito.ArgumentCaptor;
import java.time.Clock;
import java.time.Instant;
import com.ruth.inventio.entity.Rol;
import com.ruth.inventio.entity.Usuario;
import com.ruth.inventio.exception.GlobalExceptionHandler;
import com.ruth.inventio.model.EstadoRegistro;
import com.ruth.inventio.model.NombreRol;
import com.ruth.inventio.repository.RolRepository;
import com.ruth.inventio.repository.UsuarioRepository;
import com.ruth.inventio.service.AuthService;
import com.ruth.inventio.service.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.*;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.json.JsonMapper;

import java.security.SecureRandom;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HTTP, validacion, servicios, BCrypt y JWT reales; solo persistencia simulada. */
class UserManagementIntegrationTests {
    private static final String PASSWORD="Clave-inicial-123";
    private static AnnotationConfigWebApplicationContext context;
    private static MockMvc mvc;
    private static UsuarioRepository usuarios;
    private static RolRepository roles;
    private static PasswordEncoder encoder;
    private static String adminHash;
    private static PasswordResetTokenRepository resetTokens;
    private static PasswordResetMailer mailer;
    private static Clock recoveryClock;
    private static long testNumber;
    private final Map<Long,PasswordResetToken> recoveries=new HashMap<>();
    private final Map<Long,Usuario> stored=new LinkedHashMap<>();
    private String adminToken;

    @Configuration @EnableWebMvc @EnableWebSecurity
    @Import({SecurityConfig.class,JwtConfig.class,JwtUserConverter.class,SecurityErrorHandler.class,
            CurrentUser.class,AuthService.class,AuthController.class,UserService.class,UserController.class,
            PasswordRecoveryController.class,PasswordRecoveryService.class,PasswordRecoveryDispatcher.class,RecoveryRateLimiter.class,
            GlobalExceptionHandler.class})
    static class Config {
        @Bean UsuarioRepository usuarios() { return mock(UsuarioRepository.class); }
        @Bean RolRepository roles() { return mock(RolRepository.class); }
        @Bean PasswordResetTokenRepository resetTokens() { return mock(PasswordResetTokenRepository.class); }
        @Bean PasswordResetMailer mailer() { return mock(PasswordResetMailer.class); }
        @Bean("passwordRecoveryClock") Clock recoveryClock() { return mock(Clock.class); }
        @Bean("passwordRecoveryExecutor") TaskExecutor executor() { return Runnable::run; }
        @Bean PasswordRecoveryProperties recoveryProperties() {
            return new PasswordRecoveryProperties("https://inventio.example/reset-password",20);
        }
    }

    @BeforeAll static void setup() {
        byte[] key=new byte[32]; new SecureRandom().nextBytes(key);
        context=new AnnotationConfigWebApplicationContext(); context.setServletContext(new MockServletContext());
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("jwt",Map.of(
                "inventio.jwt.secret",Base64.getEncoder().encodeToString(key),
                "inventio.jwt.issuer","test","inventio.jwt.audience","clients","inventio.jwt.ttl-seconds","900")));
        context.register(Config.class); context.refresh();
        usuarios=context.getBean(UsuarioRepository.class); roles=context.getBean(RolRepository.class);
        encoder=context.getBean(PasswordEncoder.class); adminHash=encoder.encode(PASSWORD);
        resetTokens=context.getBean(PasswordResetTokenRepository.class); mailer=context.getBean(PasswordResetMailer.class);
        recoveryClock=context.getBean(Clock.class);
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @AfterAll static void close() { if (context!=null) context.close(); }
    @BeforeEach void prepare() throws Exception {
        reset(usuarios,roles,resetTokens,mailer);
        when(recoveryClock.instant()).thenReturn(Instant.now().plusSeconds(testNumber++*3600));
        when(resetTokens.findByUsuarioId(anyLong())).thenAnswer(i -> Optional.ofNullable(recoveries.get(i.getArgument(0))));
        when(resetTokens.findUsuarioIdByTokenHash(anyString())).thenAnswer(i -> recoveries.entrySet().stream()
                .filter(e -> e.getValue().getTokenHash().equals(i.getArgument(0))).map(Map.Entry::getKey).findFirst());
        when(resetTokens.save(any())).thenAnswer(i -> {
            PasswordResetToken token=i.getArgument(0); recoveries.put(token.getUsuario().getId(),token); return token;
        });
        for (NombreRol nombre:NombreRol.values()) {
            Rol rol=new Rol(); rol.setNombre(nombre);
            when(roles.findByNombre(nombre)).thenReturn(Optional.of(rol));
        }
        Usuario admin=new Usuario(); ReflectionTestUtils.setField(admin,"id",1L);
        admin.setNombre("Administrador"); admin.setEmail("admin@example.test"); admin.setPassword(adminHash);
        admin.getRoles().add(roles.findByNombre(NombreRol.ADMIN).orElseThrow()); stored.put(1L,admin);
        when(usuarios.findByEmailIgnoreCase(anyString())).thenAnswer(invocation -> stored.values().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(invocation.getArgument(0))).findFirst());
        when(usuarios.findIdByEmailIgnoreCase(anyString())).thenAnswer(invocation -> stored.values().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(invocation.getArgument(0))).map(Usuario::getId).findFirst());
        when(usuarios.findById(anyLong())).thenAnswer(invocation -> Optional.ofNullable(stored.get(invocation.getArgument(0))));
        when(usuarios.buscarParaActualizar(anyLong())).thenAnswer(invocation -> Optional.ofNullable(stored.get(invocation.getArgument(0))));
        when(usuarios.findAll()).thenAnswer(invocation -> new ArrayList<>(stored.values()));
        when(usuarios.save(any())).thenAnswer(invocation -> {
            Usuario u=invocation.getArgument(0);
            if (u.getId()==null) ReflectionTestUtils.setField(u,"id",(long)stored.size()+1);
            stored.put(u.getId(),u); return u;
        });
        adminToken=login("admin@example.test");
    }
    private static ResultMatcher noSecrets() {
        return result -> {
            String body=result.getResponse().getContentAsString();
            assertFalse(body.contains("\"password\"")); assertFalse(body.contains("\"passwordHash\""));
            assertFalse(body.contains("\"newPassword\"")); assertFalse(body.contains("$2"));
            assertFalse(body.contains(PASSWORD)); assertFalse(body.contains("tokenVersion"));
        };
    }
    private String login(String email) throws Exception {
        var result=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(JsonMapper.builder().build().writeValueAsString(Map.of("email",email,"password",PASSWORD))))
                .andExpect(status().isOk()).andExpect(noSecrets()).andReturn();
        return JsonMapper.builder().build().readTree(result.getResponse().getContentAsString()).path("accessToken").asString();
    }
    private String createBody(String role,String state) {
        return "{\"nombre\":\"Juan Perez\",\"email\":\"juan@example.test\",\"password\":\""+PASSWORD
                +"\",\"rol\":\""+role+"\",\"estado\":\""+state+"\"}";
    }
    private MvcResult create(String body) throws Exception {
        return mvc.perform(post("/api/usuarios").header("Authorization","Bearer "+adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(noSecrets()).andReturn();
    }

    @Test void fullSellerLifecycleIncludesBcryptLoginEditDeactivationAndReactivation() throws Exception {
        create(createBody("VENDEDOR","ACTIVO"));
        Usuario seller=stored.get(2L);
        assertTrue(seller.getPassword().startsWith("$2")); assertTrue(encoder.matches(PASSWORD,seller.getPassword()));
        String sellerToken=login("juan@example.test");
        mvc.perform(get("/api/usuarios").header("Authorization","Bearer "+adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[1].rol").value("VENDEDOR"))
                .andExpect(jsonPath("$[1].estado").value("ACTIVO")).andExpect(noSecrets());
        String hash=seller.getPassword();
        mvc.perform(put("/api/usuarios/2").header("Authorization","Bearer "+adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Juan Editado\",\"email\":\"juan.editado@example.test\",\"rol\":\"VENDEDOR\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Juan Editado"))
                .andExpect(jsonPath("$.email").value("juan.editado@example.test")).andExpect(noSecrets());
        assertEquals(hash,seller.getPassword());
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+sellerToken)).andExpect(status().isUnauthorized());
        String updatedToken=login("juan.editado@example.test");
        mvc.perform(patch("/api/usuarios/2/estado").header("Authorization","Bearer "+adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"INACTIVO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("INACTIVO")).andExpect(noSecrets());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"juan.editado@example.test\",\"password\":\""+PASSWORD+"\"}"))
                .andExpect(status().isUnauthorized()).andExpect(noSecrets());
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+updatedToken)).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/usuarios/2/estado").header("Authorization","Bearer "+adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ACTIVO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ACTIVO")).andExpect(noSecrets());
        assertNotNull(login("juan.editado@example.test"));
    }

    @ParameterizedTest @ValueSource(strings={"VENDEDOR","SUPERVISOR"})
    void nonAdminsCannotAdministerUsersOnEitherRoute(String role) throws Exception {
        create(createBody(role,"ACTIVO")); String token=login("juan@example.test");
        for (String route:List.of("/api/usuarios","/api/users")) {
            mvc.perform(get(route).header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
            mvc.perform(post(route).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
                    .content(createBody("ADMIN","ACTIVO"))).andExpect(status().isForbidden());
            mvc.perform(put(route+"/1").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nombre\":\"Escalado\",\"email\":\"admin@example.test\",\"rol\":\"ADMIN\"}"))
                    .andExpect(status().isForbidden());
            mvc.perform(patch(route+"/1/estado").header("Authorization","Bearer "+token)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"INACTIVO\"}"))
                    .andExpect(status().isForbidden());
        }
        assertEquals(2,stored.size()); assertEquals("Administrador",stored.get(1L).getNombre());
    }

    @Test void duplicateEmailIsRejectedCaseInsensitivelyOnCreateAndEdit() throws Exception {
        create(createBody("VENDEDOR","ACTIVO"));
        mvc.perform(post("/api/usuarios").header("Authorization","Bearer "+adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(createBody("VENDEDOR","ACTIVO").replace("juan@","JUAN@")))
                .andExpect(status().isConflict()).andExpect(noSecrets());
        mvc.perform(put("/api/usuarios/2").header("Authorization","Bearer "+adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Juan\",\"email\":\"ADMIN@example.test\",\"rol\":\"VENDEDOR\"}"))
                .andExpect(status().isConflict()).andExpect(noSecrets());
        assertEquals(2,stored.size());
    }

    @Test void creationAcceptsInactiveStateAndLegacyRolesContract() throws Exception {
        create(createBody("SUPERVISOR","INACTIVO").replace("\"rol\":\"SUPERVISOR\"","\"roles\":[\"SUPERVISOR\"]"));
        assertEquals(EstadoRegistro.INACTIVO,stored.get(2L).getEstado());
        mvc.perform(get("/api/users").header("Authorization","Bearer "+adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[1].roles[0]").value("SUPERVISOR"))
                .andExpect(jsonPath("$[1].rol").value("SUPERVISOR")).andExpect(noSecrets());
    }

    @Test void invalidDataAndClientBcryptHashesAreRejected() throws Exception {
        String valid=createBody("VENDEDOR","ACTIVO");
        for (String invalid:List.of(valid.replace("juan@example.test","invalid"),
                valid.replace("Juan Perez"," "),valid.replace(PASSWORD,"short"),
                valid.replace("VENDEDOR","UNKNOWN"),valid.replace("ACTIVO","UNKNOWN"),
                valid.replace(PASSWORD,adminHash),valid.replace(PASSWORD,"é".repeat(40)),
                valid.replace("\"rol\":\"VENDEDOR\",",""), valid.replace("juan@example.test",""))) {
            mvc.perform(post("/api/usuarios").header("Authorization","Bearer "+adminToken)
                    .contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest()).andExpect(noSecrets());
        }
        assertEquals(1,stored.size());
    }

    @Test void anonymousMissingUsersAndSelfLockoutHaveExpectedErrors() throws Exception {
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(createBody("ADMIN","ACTIVO")))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/usuarios/999").header("Authorization","Bearer "+adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Juan\",\"email\":\"juan@example.test\",\"rol\":\"VENDEDOR\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/usuarios/1/estado").header("Authorization","Bearer "+adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"INACTIVO\"}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/usuarios/1").header("Authorization","Bearer "+adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Admin\",\"email\":\"admin@example.test\",\"rol\":\"VENDEDOR\"}"))
                .andExpect(status().isConflict());
    }

    private String forgot(String email) throws Exception {
        return mvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\""+email+"\"}"))
                .andExpect(status().isOk()).andExpect(noSecrets()).andExpect(jsonPath("$.token").doesNotExist())
                .andReturn().getResponse().getContentAsString();
    }
    private String mailedToken() {
        var capture=ArgumentCaptor.forClass(PasswordResetMail.class);
        verify(mailer,atLeastOnce()).send(capture.capture());
        return capture.getValue().link().split("token=")[1];
    }
    private org.springframework.test.web.servlet.ResultActions resetPassword(String token,String password) throws Exception {
        return mvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content(JsonMapper.builder().build().writeValueAsString(Map.of("token",token,"newPassword",password))));
    }

    @Test void forgotReturnsSameResponseForExistingMissingInactiveAndMailFailure() throws Exception {
        String registered=forgot("ADMIN@example.test");
        assertEquals(registered,forgot("unknown@example.test"));
        stored.get(1L).setEstado(EstadoRegistro.INACTIVO);
        assertEquals(registered,forgot("admin@example.test"));
        verify(mailer,times(1)).send(any());
        stored.get(1L).setEstado(EstadoRegistro.ACTIVO);
        doThrow(new org.springframework.mail.MailSendException("provider failure")).when(mailer).send(any());
        assertEquals(registered,forgot("admin@example.test"));
        assertEquals(EstadoRegistro.ACTIVO,stored.get(1L).getEstado());
        assertEquals(adminHash,stored.get(1L).getPassword());
    }

    @ParameterizedTest @ValueSource(strings={"ADMIN","VENDEDOR"})
    void secureSingleUseRecoveryChangesPasswordAndRevokesOldJwt(String role) throws Exception {
        create(createBody(role,"ACTIVO")); String oldJwt=login("juan@example.test");
        forgot("juan@example.test"); String token=mailedToken();
        assertEquals(43,token.length()); assertEquals(32,Base64.getUrlDecoder().decode(token).length);
        PasswordResetToken persisted=recoveries.get(2L);
        assertEquals(RecoveryTokens.hash(token),persisted.getTokenHash()); assertNotEquals(token,persisted.getTokenHash());
        assertEquals(recoveryClock.instant().plusSeconds(1200),persisted.getExpiresAt());
        String newPassword="Nueva-clave-segura-456";
        resetPassword(token,newPassword).andExpect(status().isOk()).andExpect(noSecrets())
                .andExpect(jsonPath("$.token").doesNotExist());
        assertTrue(encoder.matches(newPassword,stored.get(2L).getPassword()));
        assertFalse(encoder.matches(PASSWORD,stored.get(2L).getPassword()));
        assertNotNull(persisted.getUsedAt()); assertEquals(1,stored.get(2L).getTokenVersion());
        resetPassword(token,"Otra-clave-segura-789").andExpect(status().isBadRequest()).andExpect(noSecrets());
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+oldJwt)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"juan@example.test\",\"password\":\""+PASSWORD+"\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"juan@example.test\",\"password\":\""+newPassword+"\"}"))
                .andExpect(status().isOk()).andExpect(noSecrets()).andExpect(jsonPath("$.user.rol").value(role));
    }

    @Test void latestRequestReplacesPreviousTokenAndExpirationBoundaryIsRejected() throws Exception {
        forgot("admin@example.test"); String first=mailedToken();
        forgot("admin@example.test"); String second=mailedToken();
        assertNotEquals(first,second); assertEquals(1,recoveries.size());
        resetPassword(first,"Nueva-clave-segura-456").andExpect(status().isBadRequest()).andExpect(noSecrets());
        when(recoveryClock.instant()).thenReturn(recoveries.get(1L).getExpiresAt());
        resetPassword(second,"Nueva-clave-segura-456").andExpect(status().isBadRequest()).andExpect(noSecrets());
        assertEquals(adminHash,stored.get(1L).getPassword());
    }

    @Test void inactiveAndAdministrativelyChangedAccountsRejectOutstandingRecovery() throws Exception {
        forgot("admin@example.test"); String token=mailedToken();
        stored.get(1L).setEstado(EstadoRegistro.INACTIVO);
        resetPassword(token,"Nueva-clave-segura-456").andExpect(status().isBadRequest());
        assertEquals(EstadoRegistro.INACTIVO,stored.get(1L).getEstado());
        stored.get(1L).setEstado(EstadoRegistro.ACTIVO); stored.get(1L).setTokenVersion(1);
        resetPassword(token,"Nueva-clave-segura-456").andExpect(status().isBadRequest());
        assertEquals(adminHash,stored.get(1L).getPassword());
    }

    @Test void weakNewPasswordsAreRejectedWithoutConsumingToken() throws Exception {
        forgot("admin@example.test"); String token=mailedToken();
        for (String password:List.of("", "short", " ".repeat(12), "a".repeat(12), "password1234", "é".repeat(40),adminHash)) {
            resetPassword(token,password).andExpect(status().isBadRequest()).andExpect(noSecrets());
        }
        assertNull(recoveries.get(1L).getUsedAt()); assertEquals(adminHash,stored.get(1L).getPassword());
        resetPassword(token,"Una frase larga segura").andExpect(status().isOk()).andExpect(noSecrets());
    }

    @Test void invalidTokensAndJwtCannotBeUsedForRecovery() throws Exception {
        for (String token:List.of("invalid",RecoveryTokens.generate(),adminToken)) {
            resetPassword(token,"Nueva-clave-segura-456").andExpect(status().isBadRequest()).andExpect(noSecrets());
        }
        verifyNoInteractions(mailer);
    }

    @Test void rateLimitedForgotKeepsGenericResponseAndLimitsEmailVolume() throws Exception {
        String response=forgot("admin@example.test");
        for (int i=0;i<6;i++) assertEquals(response,forgot("ADMIN@example.test"));
        verify(mailer,times(5)).send(any());
    }
}
