package com.domotiq.gui;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Remembers the UI theme per username in config/ui.properties (local
 * preference file, not in the database - it is a device-local setting).
 */
public final class ThemeManager {

    private static final Path FILE = Path.of("config", "ui.properties");

    private ThemeManager() {
    }

    public static Theme.Mode load(String username, Theme.Mode fallback) {
        String value = read().getProperty(key(username));
        return parse(value, fallback);
    }

    public static Theme.Mode loadDefault(Theme.Mode fallback) {
        String value = read().getProperty("theme.default");
        return parse(value, fallback);
    }

    public static void save(String username, Theme.Mode mode) {
        Properties props = read();
        props.setProperty(key(username), mode.name().toLowerCase());
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                props.store(out, "DOMOTIQ UI preferences");
            }
        } catch (IOException e) {
            System.err.println("Could not persist theme preference: " + e.getMessage());
        }
    }

    private static Theme.Mode parse(String value, Theme.Mode fallback) {
        if (value == null) {
            return fallback;
        }
        return value.equalsIgnoreCase(Theme.Mode.LIGHT.name())
                ? Theme.Mode.LIGHT : Theme.Mode.DARK;
    }

    private static String key(String username) {
        return "theme." + username;
    }

    private static Properties read() {
        Properties props = new Properties();
        if (Files.isReadable(FILE)) {
            try (InputStream in = Files.newInputStream(FILE)) {
                props.load(in);
            } catch (IOException ignored) {
                // unreadable preference file simply means "use fallback"
            }
        }
        return props;
    }
}
