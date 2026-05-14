package org.bxteam.pulsify;

import java.net.URI;

public record Dsn(String token, String projectId, String ingestUrl) {
    public static Dsn parse(String raw) {
        try {
            URI uri = new URI(raw);
            String token = uri.getUserInfo();
            String path = uri.getPath();
            String projectId = path.substring(path.lastIndexOf('/') + 1);
            if (token == null || token.isBlank())
                throw new IllegalArgumentException("Missing token in DSN");
            if (projectId.isBlank())
                throw new IllegalArgumentException("Missing projectId in DSN");
            String base = uri.getScheme() + "://" + uri.getHost()
                + (uri.getPort() != -1 ? ":" + uri.getPort() : "");
            String ingestUrl = base + "/api/v1/e/" + projectId;
            return new Dsn(token, projectId, ingestUrl);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid DSN: " + raw, e);
        }
    }
}
