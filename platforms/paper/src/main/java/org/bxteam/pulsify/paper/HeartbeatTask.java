package org.bxteam.pulsify.paper;

import org.bxteam.pulsify.StatClient;
import org.bxteam.pulsify.event.PluginInfo;
import org.bxteam.pulsify.event.ServerInfo;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class HeartbeatTask extends BukkitRunnable {
    private final StatClient client;

    HeartbeatTask(StatClient client, PulsifyPlugin plugin) {
        this.client = client;
    }

    @Override
    public void run() {
        Runtime rt = Runtime.getRuntime();
        long memUsed = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        long memMax = rt.maxMemory() / 1024 / 1024;

        ServerInfo server = new ServerInfo(
            Bukkit.getOnlinePlayers().size(),
            Bukkit.getMaxPlayers(),
            Bukkit.getTPS()[0],
            Bukkit.getServer().getAverageTickTime(),
            memUsed,
            memMax,
            Bukkit.getMinecraftVersion(),
            Bukkit.getName()
        );

        List<PluginInfo> plugins = Arrays.stream(Bukkit.getPluginManager().getPlugins())
            .map(p -> new PluginInfo(p.getName(), p.getDescription().getVersion(), p.isEnabled()))
            .collect(Collectors.toList());

        client.heartbeat(server, plugins);
    }
}
