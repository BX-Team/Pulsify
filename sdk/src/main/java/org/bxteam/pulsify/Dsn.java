package org.bxteam.pulsify;

import java.net.URI;

/**
 * Parsed Pulsify connection string. A DSN bundles the auth token, the target project and the
 * host into a single copy-pasteable URL of the form {@code https://<token>@<host>/<projectId>},
 * mirroring how Sentry distributes credentials.
 *
 * @param token     bearer token sent as {@code Authorization: Bearer <token>} on every request
 * @param projectId project that ingested events are attributed to
 * @param ingestUrl fully resolved ingest endpoint derived from the host and project id
 */
public record Dsn(String token, String projectId, String ingestUrl) {
    /**
     * Parses a raw DSN string into its components and derives the ingest URL.
     *
     * @param raw the connection string, e.g. {@code https://abc123@pulsify.bx-team.com/proj_42}
     * @return the parsed DSN
     * @throws IllegalArgumentException if the string is malformed or is missing the token or project id
     */
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
