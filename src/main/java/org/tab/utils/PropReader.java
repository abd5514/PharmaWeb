package org.tab.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropReader {
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = PropReader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                PROPS.load(in);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    public static String get(String key, String def) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) return sys;
        return PROPS.getProperty(key, def);
    }

    public static int getInt(String key, int def) {
        try {
            return Integer.parseInt(get(key, Integer.toString(def)));
        } catch (NumberFormatException nfe) {
            return def;
        }
    }

    public static boolean getBool(String key, boolean def) {
        return Boolean.parseBoolean(get(key, Boolean.toString(def)));
    }
}
