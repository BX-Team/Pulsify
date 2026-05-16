package org.bxteam.pulsify.paper;

import org.bxteam.pulsify.StatClient;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;

public final class PulsifyPlugin extends JavaPlugin {
    private StatClient client;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        String dsn = getConfig().getString("dsn");
        if (dsn == null || dsn.isBlank()) {
            getLogger().severe("DSN not set in config.yml — Pulsify disabled.");
            return;
        }
        client = StatClient.builder()
            .dsn(dsn)
            .flushInterval(Duration.ofMinutes(getConfig().getInt("flush-interval-minutes", 5)))
            .autoCollectErrors(getConfig().getBoolean("collect-errors", true))
            .ignorePlugins(getConfig().getStringList("ignored-plugins"))
            .build();

        client.pingAsync().thenAccept(ok -> {
            if (ok) getLogger().info("Pulsify: ingest reachable.");
            else getLogger().warning("Pulsify: ingest unreachable — verify DSN. Events will be queued.");
        });

        getServer().getPluginManager().registerEvents(new PlayerListener(client), this);

        long heartbeatTicks = getConfig().getInt("heartbeat-interval-seconds", 60) * 20L;
        new HeartbeatTask(client, this).runTaskTimerAsynchronously(this, heartbeatTicks, heartbeatTicks);

        getLogger().info("Pulsify connected.");
    }

    @Override
    public void onDisable() {
        if (client != null) client.close();
    }
}
