package org.bxteam.pulsify.event;

import java.util.List;

public record Heartbeat(
    String type,
    long timestamp,
    ServerInfo server,
    List<PluginInfo> plugins
) {
    public Heartbeat(ServerInfo server, List<PluginInfo> plugins) {
        this("heartbeat", System.currentTimeMillis(), server, plugins);
    }
}
