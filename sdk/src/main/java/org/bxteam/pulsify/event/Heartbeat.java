package org.bxteam.pulsify.event;

import java.util.List;

/**
 * Wire payload for a periodic server snapshot. Serialized with {@code type: "heartbeat"};
 * drives the dashboard's live server state and installed-plugin view.
 *
 * @param type      event discriminator, always {@code "heartbeat"}
 * @param timestamp snapshot time in epoch milliseconds
 * @param server    server state at the moment of the snapshot
 * @param plugins   currently installed plugins
 */
public record Heartbeat(
    String type,
    long timestamp,
    ServerInfo server,
    List<PluginInfo> plugins
) {
    /** Convenience constructor that stamps the {@code "heartbeat"} type and current timestamp. */
    public Heartbeat(ServerInfo server, List<PluginInfo> plugins) {
        this("heartbeat", System.currentTimeMillis(), server, plugins);
    }
}
