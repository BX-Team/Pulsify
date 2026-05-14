package org.bxteam.pulsify.velocity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

final class PlatformConfig {
    private final Map<String, String> values;

    private PlatformConfig(Map<String, String> values) {
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

    private static Map<String, String> parseSimpleYaml(Path file) throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        for (String line : Files.readAllLines(file)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#") || !trimmed.contains(":")) continue;
            int colon = trimmed.indexOf(':');
            String key = trimmed.substring(0, colon).trim();
            String val = trimmed.substring(colon + 1).trim();
            if (val.startsWith("\"") && val.endsWith("\"") && val.length() > 1)
                val = val.substring(1, val.length() - 1);
            result.put(key, val);
        }
        return result;
    }

    String getString(String key, String def) {
        return values.getOrDefault(key, def);
    }

    int getInt(String key, int def) {
        String v = values.get(key);
        if (v == null) return def;
        try { return Integer.parseInt(v); } catch (NumberFormatException e) { return def; }
    }

    boolean getBoolean(String key, boolean def) {
        String v = values.get(key);
        if (v == null) return def;
        return Boolean.parseBoolean(v);
    }
}
