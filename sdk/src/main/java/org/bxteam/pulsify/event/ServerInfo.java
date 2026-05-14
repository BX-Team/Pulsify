package org.bxteam.pulsify.event;

public record ServerInfo(
    int online,
    int max,
    double tps,
    double mspt,
    long memoryUsedMb,
    long memoryMaxMb,
    String version,
    String software
) {}
