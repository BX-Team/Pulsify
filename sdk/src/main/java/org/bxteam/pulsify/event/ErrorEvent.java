package org.bxteam.pulsify.event;

import org.bxteam.pulsify.ErrorLevel;

/**
 * Wire payload for a captured error. Serialized to JSON with {@code type: "error"} as the
 * discriminator the ingest endpoint routes on; nested fields are rendered in snake_case to
 * match the server schema.
 *
 * @param type      event discriminator, always {@code "error"}
 * @param timestamp capture time in epoch milliseconds
 * @param plugin    plugin the error is attributed to
 * @param error     the error details
 */
public record ErrorEvent(
    String type,
    long timestamp,
    String plugin,
    ErrorDetail error
) {
    /**
     * Inner error details. Optional fields are omitted from the JSON when null.
     *
     * @param message       error message
     * @param stacktrace    stack trace text, may be empty
     * @param level         severity
     * @param serverVersion Minecraft version, may be {@code null}
     * @param serverSoftware server software, may be {@code null}
     * @param pluginVersion version of the attributed plugin, may be {@code null}
     */
    public record ErrorDetail(
        String message,
        String stacktrace,
        ErrorLevel level,
        String serverVersion,
        String serverSoftware,
        String pluginVersion
    ) {}

    /** Convenience constructor that stamps the {@code "error"} type and current timestamp. */
    public ErrorEvent(String plugin, String message, String stacktrace, ErrorLevel level,
                      String serverVersion, String serverSoftware, String pluginVersion) {
        this("error", System.currentTimeMillis(), plugin,
            new ErrorDetail(message, stacktrace, level, serverVersion, serverSoftware, pluginVersion));
    }
}
