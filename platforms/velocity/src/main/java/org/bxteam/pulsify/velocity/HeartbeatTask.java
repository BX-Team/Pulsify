package org.bxteam.pulsify.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import org.bxteam.pulsify.StatClient;
import org.bxteam.pulsify.event.PluginInfo;
import org.bxteam.pulsify.event.ServerInfo;

import java.util.List;
import java.util.stream.Collectors;

final class HeartbeatTask implements Runnable {
    private final StatClient client;
    private final ProxyServer server;

    HeartbeatTask(StatClient client, ProxyServer server) {
        this.client = client;
        this.server = server;
    }

    @Override
    public void run() {
        Runtime rt = Runtime.getRuntime();
        long memUsed = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        long memMax = rt.maxMemory() / 1024 / 1024;

        ServerInfo serverInfo = new ServerInfo(
            server.getAllPlayers().size(),
            server.getConfiguration().getShowMaxPlayers(),
            20.0,
            0.0,
            memUsed,
            memMax,
            server.getVersion().getVersion(),
            "Velocity"
        );

        List<PluginInfo> plugins = server.getPluginManager().getPlugins().stream()
            .map(c -> new PluginInfo(
                c.getDescription().getId(),
                c.getDescription().getVersion().orElse("unknown"),
                true
            ))
            .collect(Collectors.toList());

        client.heartbeat(serverInfo, plugins);
    }
}
