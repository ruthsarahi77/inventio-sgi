package com.ruth.inventio.security;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.crypto.spec.SecretKeySpec;
import com.ruth.inventio.controller.*;
import com.ruth.inventio.entity.*;
import com.ruth.inventio.exception.GlobalExceptionHandler;
import com.ruth.inventio.model.*;
import com.ruth.inventio.repository.UsuarioRepository;
import com.ruth.inventio.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.context.annotation.*;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecurityIntegrationTests {
    private static AnnotationConfigWebApplicationContext context;
    private static MockMvc mvc;
    private static UsuarioRepository usuarios;
    private static String hash;
    private Usuario usuario;
    private static final String PASSWORD="Prueba-segura-123";

    @Configuration @EnableWebMvc @EnableWebSecurity
    @Import({SecurityConfig.class,JwtConfig.class,JwtUserConverter.class,SecurityErrorHandler.class,
            AuthService.class,AuthController.class,ProductController.class,CustomerController.class,
            UserController.class,SalesController.class,QuoteController.class,ReceiptController.class,
            InventoryController.class,GlobalExceptionHandler.class})
    static class TestConfig {
        @Bean UsuarioRepository usuarios() { return mock(UsuarioRepository.class); }
        @Bean ProductService productos() { return mock(ProductService.class); }
        @Bean CustomerService clientes() { return mock(CustomerService.class); }
        @Bean UserService users() { return mock(UserService.class); }
        @Bean SalesService ventas() { return mock(SalesService.class); }
        @Bean QuoteService proformas() { return mock(QuoteService.class); }
        @Bean ReceiptService recibos() { return mock(ReceiptService.class); }
        @Bean InventoryApiService inventario() { return mock(InventoryApiService.class); }
    }
    @BeforeAll static void setup() {
        byte[] key=new byte[32]; new java.security.SecureRandom().nextBytes(key);
        context=new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test-jwt",Map.of(
                "inventio.jwt.secret",Base64.getEncoder().encodeToString(key),
                "inventio.jwt.issuer","inventio-test","inventio.jwt.audience","test-clients",
                "inventio.jwt.ttl-seconds","900","CORS_ALLOWED_ORIGINS","http://localhost:4200")));
        context.register(TestConfig.class); context.refresh();
        usuarios=context.getBean(UsuarioRepository.class);
        hash=context.getBean(PasswordEncoder.class).encode(PASSWORD);
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @AfterAll static void close() { if(context!=null) context.close(); }
    @BeforeEach void user() {
        reset(usuarios);
        usuario=new Usuario(); ReflectionTestUtils.setField(usuario,"id",1L);
        usuario.setNombre("Administrador"); usuario.setEmail("admin@example.test"); usuario.setPassword(hash);
        role(NombreRol.ADMIN);
        when(usuarios.findByEmailIgnoreCase("admin@example.test")).thenReturn(Optional.of(usuario));
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));
    }
    private void role(NombreRol nombre) {
        usuario.getRoles().clear(); Rol r=new Rol(); r.setNombre(nombre); usuario.getRoles().add(r);
    }
    private String login() throws Exception {
        String body=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@example.test\",\"password\":\""+PASSWORD+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.usuario.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return JsonMapper.builder().build().readTree(body).path("accessToken").asString();
    }
    private String token(String issuer,String audience,Instant expiration,JwtEncoder encoder) {
        var claims=JwtClaimsSet.builder().subject("1").issuer(issuer).audience(List.of(audience))
                .issuedAt(Instant.now().minusSeconds(7200)).expiresAt(expiration).claim("ver",0L)
                .claim("roles",List.of("ADMIN")).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
    }
    @Test void loginProducesVerifiedTokenAndNoServerSession() throws Exception {
        String token=login();
        assertEquals("1",context.getBean(JwtDecoder.class).decode(token).getSubject());
        var result=mvc.perform(get("/api/products").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andReturn();
        assertNull(result.getRequest().getSession(false));
        assertTrue(hash.startsWith("$2")); assertNotEquals(PASSWORD,hash);
    }
    @Test void deniesMissingInvalidAndExpiredTokens() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        mvc.perform(get("/api/products").header("Authorization","Bearer invalid"))
                .andExpect(status().isUnauthorized());
        var encoder=context.getBean(JwtEncoder.class);
        for(String invalid:List.of(
                token("inventio-test","test-clients",Instant.now().minusSeconds(3600),encoder),
                token("wrong-issuer","test-clients",Instant.now().plusSeconds(900),encoder),
                token("inventio-test","wrong-audience",Instant.now().plusSeconds(900),encoder),
                token("inventio-test","test-clients",Instant.now().plusSeconds(900),
                        NimbusJwtEncoder.withSecretKey(new SecretKeySpec(new byte[32],"HmacSHA256"))
                                .algorithm(MacAlgorithm.HS256).build()))) {
            mvc.perform(get("/api/products").header("Authorization","Bearer "+invalid))
                    .andExpect(status().isUnauthorized());
        }
    }
    @Test void inactiveAndRevokedUsersCannotReuseToken() throws Exception {
        String token=login(); usuario.setTokenVersion(1);
        mvc.perform(get("/api/products").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
        usuario.setTokenVersion(0); usuario.setEstado(EstadoRegistro.INACTIVO);
        mvc.perform(get("/api/products").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@example.test\",\"password\":\""+PASSWORD+"\"}"))
                .andExpect(status().isUnauthorized());
    }
    @Test void badCredentialsReturnGeneric401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@example.test\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Credenciales invalidas o autenticacion requerida."));
    }
    @ParameterizedTest @EnumSource(NombreRol.class)
    void enforcesPermissionsForEachRole(NombreRol role) throws Exception {
        role(role); String token=login();
        for(String path:List.of("/api/products","/api/customers","/api/inventory","/api/quotes","/api/sales","/api/receipts")) {
            mvc.perform(get(path).header("Authorization","Bearer "+token)).andExpect(status().isOk());
        }
        mvc.perform(get("/api/users").header("Authorization","Bearer "+token))
                .andExpect(status().is(role==NombreRol.ADMIN?200:403));
        mvc.perform(post("/api/products").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"P1\",\"nombre\":\"Producto\",\"costoUnitario\":1}"))
                .andExpect(status().is(role==NombreRol.ADMIN?201:403));
        mvc.perform(post("/api/customers").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"identificacion\":\"001\",\"nombre\":\"Cliente\"}"))
                .andExpect(status().is(role==NombreRol.SUPERVISOR?403:201));
        mvc.perform(post("/api/sales").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"proformaId\":1}"))
                .andExpect(status().is(role==NombreRol.SUPERVISOR?403:201));
        mvc.perform(patch("/api/sales/1/cancel").header("Authorization","Bearer "+token))
                .andExpect(status().is(role==NombreRol.ADMIN?200:403));
        mvc.perform(get("/api/inventory/kardex/1").header("Authorization","Bearer "+token))
                .andExpect(status().is(role==NombreRol.VENDEDOR?403:200));
    }
    @Test void rolesInTokenCannotOverrideCurrentDatabasePermissions() throws Exception {
        role(NombreRol.VENDEDOR);
        String token=token("inventio-test","test-clients",Instant.now().plusSeconds(900),context.getBean(JwtEncoder.class));
        mvc.perform(get("/api/users").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
    }
    @Test void corsPermitsOnlyConfiguredOrigins() throws Exception {
        mvc.perform(options("/api/sales").header("Origin","http://localhost:4200")
                .header("Access-Control-Request-Method","POST").header("Access-Control-Request-Headers","authorization,content-type"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:4200"));
        mvc.perform(options("/api/sales").header("Origin","https://untrusted.test")
                .header("Access-Control-Request-Method","POST")).andExpect(status().isForbidden());
    }
    @Test void ownershipChecksRejectOtherSellerDocuments() {
        var current=new CurrentUser(usuarios);
        var principal=new UsuarioPrincipal(1L,java.util.Set.of(NombreRol.VENDEDOR));
        var auth=new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal,null,List.of());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            assertDoesNotThrow(() -> current.verificarPropietario(1L));
            assertThrows(org.springframework.security.access.AccessDeniedException.class,() -> current.verificarPropietario(2L));
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    }
}
