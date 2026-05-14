package org.bxteam.pulsify.event;

import org.bxteam.pulsify.ErrorLevel;

public record ErrorEvent(
    String type,
    long timestamp,
    String plugin,
    ErrorDetail error
) {
    public record ErrorDetail(String message, String stacktrace, ErrorLevel level) {}

    public ErrorEvent(String plugin, String message, String stacktrace, ErrorLevel level) {
        this("error", System.currentTimeMillis(), plugin,
            new ErrorDetail(message, stacktrace, level));
    }
}
