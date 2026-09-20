package com.ruth.inventio.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DatabaseConnectionVerifier implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConnectionVerifier.class);
    private final DataSource dataSource;

    public DatabaseConnectionVerifier(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(5);
            try (ResultSet result = statement.executeQuery("SELECT 1")) {
                if (!result.next() || result.getInt(1) != 1) {
                    throw new IllegalStateException("La verificacion de PostgreSQL no fue satisfactoria.");
                }
            }
        }
        log.info("Conexion con PostgreSQL verificada correctamente (SELECT 1).");
    }
}
