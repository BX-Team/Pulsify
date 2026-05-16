package org.bxteam.pulsify.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.bxteam.pulsify.StatClient;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Plugin(id = "pulsify", name = "Pulsify", version = "1.0.0",
        description = "Pulsify analytics SDK for Velocity")
public final class PulsifyPlugin {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private StatClient client;

    @Inject
    public PulsifyPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        PlatformConfig config;
        try {
            config = PlatformConfig.load(dataDirectory, "config.yml");
        } catch (IOException e) {
            logger.error("Failed to load config: {}", e.getMessage());
            return;
        }

        String dsn = config.getString("dsn", "");
        if (dsn.isBlank()) {
            logger.error("DSN not set in config.yml — Pulsify disabled.");
            return;
        }

        client = StatClient.builder()
            .dsn(dsn)
            .flushInterval(Duration.ofMinutes(config.getInt("flush-interval-minutes", 5)))
            .autoCollectErrors(config.getBoolean("collect-errors", true))
            .ignorePlugins(config.getStringList("ignored-plugins"))
            .build();

        client.pingAsync().thenAccept(ok -> {
            if (ok) logger.info("Pulsify: ingest reachable.");
            else logger.warn("Pulsify: ingest unreachable — verify DSN. Events will be queued.");
        });

        server.getEventManager().register(this, new PlayerListener(client));

        int heartbeatSeconds = config.getInt("heartbeat-interval-seconds", 60);
        server.getScheduler().buildTask(this, new HeartbeatTask(client, server))
            .repeat(heartbeatSeconds, TimeUnit.SECONDS)
            .schedule();

        logger.info("Pulsify connected.");
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        if (client != null) client.close();
    }
}
