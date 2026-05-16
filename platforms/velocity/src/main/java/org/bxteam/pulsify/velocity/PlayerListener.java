package org.bxteam.pulsify.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import org.bxteam.pulsify.StatClient;

final class PlayerListener {
    private final StatClient client;

    PlayerListener(StatClient client) {
        this.client = client;
    }

    @Subscribe
    public void onLogin(PostLoginEvent e) {
        java.net.InetSocketAddress addr = e.getPlayer().getRemoteAddress();
        String ip = addr != null ? addr.getAddress().getHostAddress() : null;
        client.playerJoin(
            e.getPlayer().getUniqueId().toString(),
            String.valueOf(e.getPlayer().getProtocolVersion().getMostRecentSupportedVersion()),
            ip
        );
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent e) {
        client.playerQuit(e.getPlayer().getUniqueId().toString());
    }
}
