package com.mohistmc.silkard.util;

import com.mohistmc.silkard.api.ColorAPI;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class I18n {

    public static Logger LOGGER = LogManager.getLogger("Silkard");

    private static ResourceBundle bundle;

    private I18n() {
    }

    private static ResourceBundle bundle() {
        ResourceBundle local = bundle;
        if (local != null) {
            return local;
        }
        synchronized (I18n.class) {
            if (bundle == null) {
                bundle = load();
            }
            return bundle;
        }
    }

    private static ResourceBundle load() {
        ClassLoader loader = I18n.class.getClassLoader();
        String suffix = Locale.getDefault().toString();
        ResourceBundle fallback = read(loader, "lang/message.properties");
        ResourceBundle localized = suffix.isEmpty() ? null : read(loader, "lang/message_" + suffix + ".properties");
        return localized != null ? localized : fallback;
    }

    private static ResourceBundle read(ClassLoader loader, String path) {
        try (InputStream in = loader.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return new PropertyResourceBundle(reader);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load {}", path, e);
            return null;
        }
    }

    public static String as(String key) {
        ResourceBundle rb = bundle();
        String raw = key;
        if (rb != null) {
            try {
                raw = rb.getString(key);
            } catch (MissingResourceException ignored) {
            }
        }
        return ColorAPI.string(raw);
    }

    public static String as(String key, Object... objects) {
        String raw = as(key);
        if (objects == null || objects.length == 0) {
            return raw;
        }
        try {
            return String.format(raw, objects);
        } catch (Exception e) {
            return raw;
        }
    }
}
