package org.bxteam.pulsify.velocity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PlatformConfig {
    private final Map<String, Object> values;

    private PlatformConfig(Map<String, Object> values) {
        this.values = values;
    }

    static PlatformConfig load(Path dataDir, String resourceName) throws IOException {
        Path file = dataDir.resolve("config.yml");
        if (!Files.exists(file)) {
            Files.createDirectories(dataDir);
            try (InputStream in = PlatformConfig.class.getResourceAsStream("/" + resourceName)) {
                if (in != null) Files.copy(in, file);
            }
        }
        return new PlatformConfig(parseSimpleYaml(file));
    }

    private static Map<String, Object> parseSimpleYaml(Path file) throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        String pendingListKey = null;
        List<String> pendingList = null;

        for (String rawLine : Files.readAllLines(file)) {
            String trimmed = rawLine.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            if (trimmed.startsWith("- ") || trimmed.equals("-")) {
                if (pendingList != null) {
                    String item = trimmed.length() > 1 ? trimmed.substring(1).trim() : "";
                    if (item.startsWith("\"") && item.endsWith("\"") && item.length() > 1)
                        item = item.substring(1, item.length() - 1);
                    if (!item.isEmpty()) pendingList.add(item);
                }
                continue;
            }

            if (pendingListKey != null) {
                result.put(pendingListKey, pendingList);
                pendingListKey = null;
                pendingList = null;
            }

            if (!trimmed.contains(":")) continue;
            int colon = trimmed.indexOf(':');
            String key = trimmed.substring(0, colon).trim();
            String val = trimmed.substring(colon + 1).trim();

            if (val.isEmpty()) {
                pendingListKey = key;
                pendingList = new ArrayList<>();
            } else if (val.startsWith("[") && val.endsWith("]")) {
                List<String> inline = new ArrayList<>();
                String inner = val.substring(1, val.length() - 1).trim();
                if (!inner.isEmpty()) {
                    for (String part : inner.split(",")) {
                        String item = part.trim();
                        if (item.startsWith("\"") && item.endsWith("\"") && item.length() > 1)
                            item = item.substring(1, item.length() - 1);
                        if (!item.isEmpty()) inline.add(item);
                    }
                }
                result.put(key, inline);
            } else {
                if (val.startsWith("\"") && val.endsWith("\"") && val.length() > 1)
                    val = val.substring(1, val.length() - 1);
                result.put(key, val);
            }
        }
        if (pendingListKey != null) {
            result.put(pendingListKey, pendingList);
        }
        return result;
    }

    String getString(String key, String def) {
        Object v = values.get(key);
        return v instanceof String ? (String) v : def;
    }

    int getInt(String key, int def) {
        Object v = values.get(key);
        if (!(v instanceof String)) return def;
        try { return Integer.parseInt((String) v); } catch (NumberFormatException e) { return def; }
    }

    boolean getBoolean(String key, boolean def) {
        Object v = values.get(key);
        if (!(v instanceof String)) return def;
        return Boolean.parseBoolean((String) v);
    }

    @SuppressWarnings("unchecked")
    List<String> getStringList(String key) {
        Object v = values.get(key);
        if (v instanceof List) return (List<String>) v;
        return Collections.emptyList();
    }
}
