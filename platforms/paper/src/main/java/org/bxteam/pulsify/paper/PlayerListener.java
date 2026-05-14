package org.bxteam.pulsify.paper;

import org.bxteam.pulsify.StatClient;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerListener implements Listener {
    private final StatClient client;

    PlayerListener(StatClient client) {
        this.client = client;
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent e) {
        String ip = e.getAddress() != null ? e.getAddress().getHostAddress() : null;
        client.playerJoin(
            e.getPlayer().getUniqueId().toString(),
            Bukkit.getMinecraftVersion(),
            ip
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        client.playerQuit(e.getPlayer().getUniqueId().toString());
    }
}
