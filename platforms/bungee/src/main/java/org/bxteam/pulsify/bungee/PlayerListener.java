package org.bxteam.pulsify.bungee;

import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.bxteam.pulsify.StatClient;

final class PlayerListener implements Listener {
    private final StatClient client;

    PlayerListener(StatClient client) {
        this.client = client;
    }

    @EventHandler
    public void onLogin(PostLoginEvent e) {
        java.net.InetSocketAddress addr = e.getPlayer().getPendingConnection().getAddress();
        String ip = addr != null ? addr.getAddress().getHostAddress() : null;
        client.playerJoin(
            e.getPlayer().getUniqueId().toString(),
            String.valueOf(e.getPlayer().getPendingConnection().getVersion()),
            ip
        );
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent e) {
        client.playerQuit(e.getPlayer().getUniqueId().toString());
    }
}
