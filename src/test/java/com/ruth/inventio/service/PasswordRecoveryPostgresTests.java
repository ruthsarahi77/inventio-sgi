package com.ruth.inventio.service;

import com.ruth.inventio.config.PasswordRecoveryProperties;
import com.ruth.inventio.entity.Rol;
import com.ruth.inventio.entity.Usuario;
import com.ruth.inventio.exception.ReglaNegocioException;
import com.ruth.inventio.model.NombreRol;
import com.ruth.inventio.repository.*;
import com.ruth.inventio.security.RecoveryTokens;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/** Opcional: PostgreSQL real en un esquema aislado; nunca envia correo. */
@EnabledIfEnvironmentVariable(named="INVENTIO_TEST_DB_URL", matches=".+")
class PasswordRecoveryPostgresTests {
    private static DriverManagerDataSource dataSource;
    private static LocalContainerEntityManagerFactoryBean factory;
    private static TransactionTemplate transaction;
    private static UsuarioRepository usuarios;
    private static PasswordResetTokenRepository tokens;
    private static PasswordRecoveryService service;
    private static String schema;
    private static boolean created;
    private static Long userId;
    private static final BCryptPasswordEncoder ENCODER=new BCryptPasswordEncoder(4);

    @BeforeAll static void setup() throws Exception {
        dataSource=new DriverManagerDataSource(System.getenv("INVENTIO_TEST_DB_URL"),
                System.getenv("INVENTIO_TEST_DB_USERNAME"),System.getenv("INVENTIO_TEST_DB_PASSWORD"));
        schema="inventio_recovery_test_"+UUID.randomUUID().toString().replace("-","");
        try(var connection=dataSource.getConnection(); var statement=connection.createStatement()) {
            statement.execute("CREATE SCHEMA "+schema); created=true;
        }
        factory=new LocalContainerEntityManagerFactoryBean(); factory.setDataSource(dataSource);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter()); factory.setPackagesToScan("com.ruth.inventio.entity");
        factory.setJpaPropertyMap(Map.of("hibernate.default_schema",schema,
                "jakarta.persistence.schema-generation.database.action","create"));
        factory.afterPropertiesSet();
        var manager=new JpaTransactionManager(factory.getObject()); transaction=new TransactionTemplate(manager);
        var repositories=new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(factory.getObject()));
        usuarios=repositories.getRepository(UsuarioRepository.class); tokens=repositories.getRepository(PasswordResetTokenRepository.class);
        var roles=repositories.getRepository(RolRepository.class);
        userId=transaction.execute(status -> {
            Rol rol=new Rol(); rol.setNombre(NombreRol.VENDEDOR); roles.save(rol);
            Usuario user=new Usuario(); user.setNombre("Recovery test"); user.setEmail("recovery@example.test");
            user.setPassword(ENCODER.encode("Initial-test-password")); user.getRoles().add(rol); return usuarios.save(user).getId();
        });
        var advice=new TransactionInterceptor(); advice.setTransactionManager(manager);
        advice.setTransactionAttributeSource(new AnnotationTransactionAttributeSource()); advice.afterPropertiesSet();
        var proxy=new ProxyFactory(new PasswordRecoveryService(usuarios,tokens,ENCODER,
                new PasswordRecoveryProperties("https://app.example/reset",20),Clock.systemUTC()));
        proxy.addAdvice(advice); service=(PasswordRecoveryService)proxy.getProxy();
    }
    @AfterAll static void cleanup() throws Exception {
        if(factory!=null) factory.destroy();
        if(created) {
            // Solo el esquema aleatorio creado por esta clase.
            assertTrue(schema.matches("inventio_recovery_test_[a-f0-9]{32}"));
            try(var connection=dataSource.getConnection(); var statement=connection.createStatement()) {
                statement.execute("DROP SCHEMA "+schema+" CASCADE");
            }
        }
    }

    @Test void persistedReplacementRejectsOldTokenAndOnlyOneConcurrentResetCanCommit() throws Exception {
        String first=service.issue("recovery@example.test").orElseThrow().link().split("token=")[1];
        String second=service.issue("recovery@example.test").orElseThrow().link().split("token=")[1];
        int count=transaction.execute(status -> tokens.findAll().size());
        assertEquals(1,count);
        assertEquals(RecoveryTokens.hash(second),transaction.execute(status -> tokens.findByUsuarioId(userId).orElseThrow().getTokenHash()));
        assertThrows(ReglaNegocioException.class,() -> service.reset(first,"Another-test-password"));
        var ready=new CountDownLatch(2); var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            Callable<Boolean> attempt=() -> {
                ready.countDown(); assertTrue(start.await(10,TimeUnit.SECONDS));
                try { service.reset(second,"Changed-test-password"); return true; }
                catch(ReglaNegocioException ex) { return false; }
            };
            var one=pool.submit(attempt); var two=pool.submit(attempt);
            assertTrue(ready.await(10,TimeUnit.SECONDS)); start.countDown();
            assertNotEquals(one.get(15,TimeUnit.SECONDS),two.get(15,TimeUnit.SECONDS));
        }
        transaction.executeWithoutResult(status -> {
            Usuario user=usuarios.findById(userId).orElseThrow();
            assertTrue(ENCODER.matches("Changed-test-password",user.getPassword())); assertEquals(1,user.getTokenVersion());
            assertNotNull(tokens.findByUsuarioId(userId).orElseThrow().getUsedAt());
        });
    }
}
