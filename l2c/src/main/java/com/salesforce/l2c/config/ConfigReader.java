package com.salesforce.l2c.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Loads config.properties once. Any value can be overridden at runtime with a
 * JVM system property of the same key, e.g. -DsfPassword=secret. This lets you
 * keep secrets out of source control.
 */
public final class ConfigReader {

    private static final Properties PROPS = new Properties();

    static {
        try (FileInputStream fis = new FileInputStream("src/test/resources/config.properties")) {
            PROPS.load(fis);
        } catch (IOException e) {
            throw new RuntimeException("Could not load src/test/resources/config.properties", e);
        }
    }

    private ConfigReader() { }

    public static String get(String key) {
        String override = System.getProperty(key);
        return (override != null) ? override : PROPS.getProperty(key);
    }
}
