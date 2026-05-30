package org.bxteam.pulsify.event;

import org.bxteam.pulsify.ErrorLevel;

public record ErrorEvent(
    String type,
    long timestamp,
    String plugin,
    ErrorDetail error
) {
    public record ErrorDetail(
        String message,
        String stacktrace,
        ErrorLevel level,
        String serverVersion,
        String serverSoftware,
        String pluginVersion
    ) {}

    public ErrorEvent(String plugin, String message, String stacktrace, ErrorLevel level,
                      String serverVersion, String serverSoftware, String pluginVersion) {
        this("error", System.currentTimeMillis(), plugin,
            new ErrorDetail(message, stacktrace, level, serverVersion, serverSoftware, pluginVersion));
    }
}
