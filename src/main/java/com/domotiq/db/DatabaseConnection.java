package com.domotiq.db;

import com.domotiq.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Central JDBC connection provider. Desktop-scale app, so a single lazily
 * opened connection that survives for the session is sufficient; swap this
 * for a pooled DataSource if the app ever becomes multi-threaded or remote.
 */
public final class DatabaseConnection {

    private static DatabaseConfig config;
    private static Connection connection;

    private DatabaseConnection() {
    }

    public static synchronized Connection get() throws SQLException {
        if (config == null) {
            config = DatabaseConfig.load();
        }
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(
                    config.getUrl(), config.getUser(), config.getPassword());
        }
        return connection;
    }

    public static synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
        connection = null;
    }

    /** Throws a friendly error if MySQL is unreachable. */
    public static void verify() throws SQLException {
        get().isValid(5);
    }
}
