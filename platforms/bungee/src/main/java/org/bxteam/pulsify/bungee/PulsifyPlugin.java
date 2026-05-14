package org.bxteam.pulsify.bungee;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.bxteam.pulsify.StatClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public final class PulsifyPlugin extends Plugin {
    private StatClient client;

    @Override
    public void onEnable() {
        Configuration config = loadConfig();
        if (config == null) return;

        String dsn = config.getString("dsn", "");
        if (dsn.isBlank()) {
            getLogger().severe("DSN not set in config.yml — Pulsify disabled.");
            return;
        }

        client = StatClient.builder()
            .dsn(dsn)
            .flushInterval(Duration.ofMinutes(config.getInt("flush-interval-minutes", 5)))
            .autoCollectErrors(config.getBoolean("collect-errors", true))
            .build();

        client.pingAsync().thenAccept(ok -> {
            if (ok) getLogger().info("Pulsify: ingest reachable.");
            else getLogger().warning("Pulsify: ingest unreachable — verify DSN. Events will be queued.");
        });

        getProxy().getPluginManager().registerListener(this, new PlayerListener(client));

        int heartbeatSeconds = config.getInt("heartbeat-interval-seconds", 60);
        getProxy().getScheduler().schedule(this,
            new HeartbeatTask(client, getProxy()),
            heartbeatSeconds, heartbeatSeconds, TimeUnit.SECONDS);

        getLogger().info("Pulsify connected.");
    }

    @Override
    public void onDisable() {
        if (client != null) client.close();
    }

    private Configuration loadConfig() {
        try {
            if (!getDataFolder().exists()) getDataFolder().mkdir();
            File file = new File(getDataFolder(), "config.yml");
            if (!file.exists()) {
                try (InputStream in = getResourceAsStream("config.yml")) {
                    if (in != null) Files.copy(in, file.toPath());
                }
            }
            return ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        } catch (IOException e) {
            getLogger().severe("Failed to load config: " + e.getMessage());
            return null;
        }
    }
}
