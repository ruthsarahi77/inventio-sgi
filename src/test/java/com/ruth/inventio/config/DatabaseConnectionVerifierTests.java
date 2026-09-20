package com.ruth.inventio.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DatabaseConnectionVerifierTests {

    @Test
    void verifiesConnectionAndClosesResources() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet result = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT 1")).thenReturn(result);
        when(result.next()).thenReturn(true);
        when(result.getInt(1)).thenReturn(1);

        new DatabaseConnectionVerifier(dataSource).run(null);

        verify(result).close();
        verify(statement).close();
        verify(connection).close();
    }

    @Test
    void propagatesConnectionFailureToStopStartup() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection unavailable"));

        assertThrows(SQLException.class, () -> new DatabaseConnectionVerifier(dataSource).run(null));
    }
}
