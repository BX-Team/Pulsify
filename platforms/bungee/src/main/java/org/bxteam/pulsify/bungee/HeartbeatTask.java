package org.bxteam.pulsify.bungee;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
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
            server.getOnlineCount(),
            server.getConfig().getPlayerLimit(),
            20.0,
            0.0,
            memUsed,
            memMax,
            server.getVersion(),
            "BungeeCord"
        );

        List<PluginInfo> plugins = server.getPluginManager().getPlugins().stream()
            .map(p -> {
                String version = p.getDescription().getVersion();
                return new PluginInfo(
                    p.getDescription().getName(),
                    version != null ? version : "unknown",
                    true
                );
            })
            .collect(Collectors.toList());

        client.heartbeat(serverInfo, plugins);
    }
}
