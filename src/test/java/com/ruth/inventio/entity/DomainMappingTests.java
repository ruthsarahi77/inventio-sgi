package com.ruth.inventio.entity;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.ruth.inventio.repository.ClienteRepository;
import com.ruth.inventio.repository.MovimientoInventarioRepository;
import com.ruth.inventio.repository.ProductoRepository;
import com.ruth.inventio.repository.ProformaDetalleRepository;
import com.ruth.inventio.repository.ProformaRepository;
import com.ruth.inventio.repository.ReciboRepository;
import com.ruth.inventio.repository.RolRepository;
import com.ruth.inventio.repository.UsuarioRepository;
import com.ruth.inventio.repository.VentaDetalleRepository;
import com.ruth.inventio.repository.VentaRepository;
import com.ruth.inventio.repository.PasswordResetTokenRepository;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import static org.junit.jupiter.api.Assertions.*;

class DomainMappingTests {

    private static final List<Class<?>> ENTITIES = List.of(Usuario.class, Rol.class,
            Cliente.class, Producto.class, MovimientoInventario.class, Proforma.class,
            ProformaDetalle.class, Venta.class, VentaDetalle.class, Recibo.class, PasswordResetToken.class);

    @Test
    void validatesMappingsRepositoriesAndGeneratesPostgresDdlWithoutDatabase() throws Exception {
        StringWriter ddl = new StringWriter();
        Configuration configuration = new Configuration();
        ENTITIES.forEach(configuration::addAnnotatedClass);
        configuration.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        configuration.setProperty("hibernate.boot.allow_jdbc_metadata_access", "false");
        configuration.setProperty("hibernate.hbm2ddl.auto", "none");
        configuration.setProperty("jakarta.persistence.schema-generation.database.action", "none");
        configuration.setProperty("jakarta.persistence.schema-generation.scripts.action", "create");
        configuration.getProperties().put("jakarta.persistence.schema-generation.scripts.create-target", ddl);

        // Sin URL, credenciales ni conexiones: se validan los mapeos y se escribe solo un script.
        try (var factory = configuration.buildSessionFactory();
             var entityManager = factory.createEntityManager()) {
            assertEquals(11, factory.getMetamodel().getEntities().size());
            JpaRepositoryFactory repositories = new JpaRepositoryFactory(entityManager);
            for (Class<?> repository : List.of(UsuarioRepository.class, RolRepository.class,
                    ClienteRepository.class, ProductoRepository.class, MovimientoInventarioRepository.class,
                    ProformaRepository.class, ProformaDetalleRepository.class, VentaRepository.class,
                    VentaDetalleRepository.class, ReciboRepository.class, PasswordResetTokenRepository.class)) {
                assertNotNull(repositories.getRepository(repository));
                assertTrue(java.util.Arrays.stream(repository.getMethods())
                        .noneMatch(method -> method.getName().startsWith("delete")));
            }
        }

        String sql = ddl.toString().toLowerCase();
        Set<String> tables = Pattern.compile("create table (\\w+)").matcher(sql).results()
                .map(match -> match.group(1)).collect(Collectors.toSet());
        assertEquals(Set.of("usuarios", "roles", "usuarios_roles", "clientes", "productos",
                "movimientos_inventario", "proformas", "proforma_detalles", "ventas",
                "venta_detalles", "recibos", "password_reset_tokens"), tables);
        assertTrue(sql.contains("token_hash varchar(64) not null unique"));
        assertTrue(sql.contains("cantidad > 0"));
        assertTrue(sql.contains("costo_unitario >= 0"));
        assertTrue(sql.contains("monto > 0"));
        assertTrue(sql.contains("numeric(19,2)"));
        assertTrue(sql.contains("numeric(19,3)"));
        assertTrue(sql.contains("vendedor_id bigint not null"));
        assertTrue(sql.contains("foreign key (venta_id) references ventas"));
        assertFalse(sql.contains("on delete cascade"));
        assertFalse(sql.contains("drop table"));
        assertFalse(sql.contains("stock"));
        Files.writeString(Path.of("target", "inventio-schema-preview.sql"), ddl.toString());
    }
}
