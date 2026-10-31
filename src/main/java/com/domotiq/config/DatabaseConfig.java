package com.domotiq.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Resolves JDBC credentials without hardcoding them.
 * Priority: environment variables > config/domotiq.properties > fail.
 */
public final class DatabaseConfig {

    private final String url;
    private final String user;
    private final String password;

    private DatabaseConfig(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public static DatabaseConfig load() {
        Properties props = new Properties();
        Path file = Path.of("config", "domotiq.properties");
        if (Files.isReadable(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Could not read " + file.toAbsolutePath(), e);
            }
        }

        String url = firstNonBlank(
                System.getenv("DOMOTIQ_DB_URL"), props.getProperty("db.url"));
        String user = firstNonBlank(
                System.getenv("DOMOTIQ_DB_USER"), props.getProperty("db.user"));
        String password = firstNonBlank(
                System.getenv("DOMOTIQ_DB_PASSWORD"), props.getProperty("db.password"));

        if (url == null || user == null || password == null
                || password.startsWith("your_")) {
            throw new IllegalStateException("""
                    DOMOTIQ database credentials are not configured.
                    Set environment variables DOMOTIQ_DB_URL / DOMOTIQ_DB_USER / DOMOTIQ_DB_PASSWORD,
                    or copy config/domotiq.properties.example to config/domotiq.properties
                    and fill in real values.""");
        }
        return new DatabaseConfig(url, user, password);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }

    public String getUrl() { return url; }
    public String getUser() { return user; }
    public String getPassword() { return password; }
}
