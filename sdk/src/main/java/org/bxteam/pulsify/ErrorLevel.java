package org.bxteam.pulsify;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ErrorLevel {
    WARNING("warning"),
    ERROR("error"),
    FATAL("fatal");

    private final String value;

    ErrorLevel(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
