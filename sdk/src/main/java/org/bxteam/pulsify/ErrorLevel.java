package org.bxteam.pulsify;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Severity of a captured error. The wire format uses the lowercase {@link #getValue() value}
 * (e.g. {@code "error"}), which the ingest schema validates as one of {@code warning},
 * {@code error} or {@code fatal}.
 */
public enum ErrorLevel {
    WARNING("warning"),
    ERROR("error"),
    FATAL("fatal");

    private final String value;

    ErrorLevel(String value) {
        this.value = value;
    }

    /** Lowercase wire value serialized into the JSON payload. */
    @JsonValue
    public String getValue() {
        return value;
    }
}
