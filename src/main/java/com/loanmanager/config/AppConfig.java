package com.loanmanager.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Application settings, resolved in increasing order of priority:
 * <ol>
 *   <li>{@code config.properties} bundled on the classpath (defaults)</li>
 *   <li>{@code config.properties} in the working directory (local overrides, not committed)</li>
 *   <li>JVM system properties, e.g. {@code -Ddb.password=secret}</li>
 * </ol>
 */
public final class AppConfig {

    private static final String FILE_NAME = "config.properties";

    private final Properties props = new Properties();

    private AppConfig() {
    }

    public static AppConfig load() {
        AppConfig config = new AppConfig();
        config.loadClasspathDefaults();
        config.loadLocalOverrides();
        config.applySystemPropertyOverrides();
        return config;
    }

    public String dbUrl() {
        return require("db.url");
    }

    public String dbUser() {
        return require("db.user");
    }

    public String dbPassword() {
        return props.getProperty("db.password", "");
    }

    public String adminUsername() {
        return require("admin.username");
    }

    public String adminPassword() {
        return require("admin.password");
    }

    public int maxFailedAttempts() {
        return Integer.parseInt(props.getProperty("security.max-failed-attempts", "3"));
    }

    private String require(String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required setting '" + key + "' in " + FILE_NAME);
        }
        return value.trim();
    }

    private void loadClasspathDefaults() {
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream(FILE_NAME)) {
            if (in == null) {
                throw new IllegalStateException(FILE_NAME + " not found on classpath");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read bundled " + FILE_NAME, e);
        }
    }

    /** Lets any known key be overridden on the command line: {@code java -Ddb.password=secret -jar ...}. */
    private void applySystemPropertyOverrides() {
        for (String key : props.stringPropertyNames()) {
            String override = System.getProperty(key);
            if (override != null) {
                props.setProperty(key, override);
            }
        }
    }

    private void loadLocalOverrides() {
        Path local = Path.of(FILE_NAME);
        if (!Files.isRegularFile(local)) {
            return;
        }
        try (InputStream in = Files.newInputStream(local)) {
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + local.toAbsolutePath(), e);
        }
    }
}
