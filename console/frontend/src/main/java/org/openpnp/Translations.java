package org.openpnp;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Translations {
    private static final String BUNDLE_NAME = "org.openpnp.translations"; //$NON-NLS-1$

    private static ResourceBundle bundle() {
        // ResourceBundle caches bundles; do not freeze the OS locale before preferences load.
        return ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault(), new UTF8Control());
    }

    private Translations() {
    }

    public static String getString(String key) {
        try {
            return bundle().getString(key);
        } catch (MissingResourceException e) {
            return '!' + key + '!';
        }
    }

    /** Translate annotation text at the display boundary, keeping annotation constants intact. */
    public static String translate(String original) {
        if (original == null || original.isEmpty()) {
            return original;
        }
        String key = "Local." + org.apache.commons.codec.digest.DigestUtils
                .sha256Hex(original).substring(0, 16);
        ResourceBundle resources = bundle();
        return resources.containsKey(key) ? resources.getString(key) : original;
    }

    public static String format(String key, Object... arguments) {
        return String.format(Locale.ROOT, getString(key), arguments);
    }

    /** A display label only: the model, enum name and serialized value are never changed. */
    public static String display(Object value) {
        if (value instanceof Enum<?> && Locale.SIMPLIFIED_CHINESE.getLanguage().equals(Locale.getDefault().getLanguage())) {
            Enum<?> item = (Enum<?>) value;
            String key = "Display." + item.getDeclaringClass().getSimpleName() + "." + item.name();
            if (bundle().containsKey(key)) {
                return getString(key);
            }
        }
        return value == null ? "" : value.toString();
    }

    public static String visionLabel(String kind, String name) {
        String key = "Vision." + kind + "." + name;
        return bundle().containsKey(key) ? getString(key) : name;
    }

    public static String displayClass(Class<?> type) {
        String name = type.getSimpleName();
        String label = visionLabel("Class", name);
        return label.equals(name) ? name : label + " (" + name + ")";
    }

    /** Keep diagnostic identity independent of its displayed language. */
    public static final class Message {
        private final String key;
        private final Object[] arguments;

        private Message(String key, Object[] arguments) {
            this.key = key;
            // Match Java string concatenation: capture each argument at message creation time.
            this.arguments = Arrays.stream(arguments)
                    .map(value -> value instanceof Message ? value : String.valueOf(value)).toArray();
        }

        public String original() {
            ResourceBundle english = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ROOT, new UTF8Control());
            return String.format(Locale.ROOT, english.getString(key),
                    Arrays.stream(arguments).map(Translations::original).toArray());
        }

        @Override
        public String toString() {
            return format(key, arguments);
        }
    }

    public static Message message(String key, Object... arguments) {
        return new Message(key, arguments);
    }

    public static String original(Object text) {
        return text instanceof Message ? ((Message) text).original() : String.valueOf(text);
    }

    public static class UTF8Control extends ResourceBundle.Control {
        public ResourceBundle newBundle
                (String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
                throws IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            ResourceBundle bundle = null;
            InputStream stream = null;
            if (reload) {
                URL url = loader.getResource(resourceName);
                if (url != null) {
                    URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }
            if (stream != null) {
                try {
                    bundle = new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
                } finally {
                    stream.close();
                }
            }
            return bundle;
        }
    }
}
