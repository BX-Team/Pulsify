package org.bxteam.pulsify.event;

/**
 * Server state snapshot carried by a {@link Heartbeat}.
 *
 * @param online       players currently online
 * @param max          maximum player slots
 * @param tps          ticks per second
 * @param mspt         milliseconds per tick
 * @param memoryUsedMb used heap in megabytes
 * @param memoryMaxMb  maximum heap in megabytes
 * @param version      Minecraft version
 * @param software     server software (e.g. "Paper")
 */
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
