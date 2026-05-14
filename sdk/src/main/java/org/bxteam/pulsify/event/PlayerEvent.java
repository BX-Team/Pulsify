package org.bxteam.pulsify.event;

public record PlayerEvent(
    String type,
    long timestamp,
    String event,
    Payload payload
) {
    public record Payload(String playerUuid, String clientVersion, String playerIp) {}

    public static PlayerEvent join(String uuid, String clientVersion, String playerIp) {
        return new PlayerEvent("event", System.currentTimeMillis(), "player_join",
            new Payload(uuid, clientVersion, playerIp));
    }

    public static PlayerEvent quit(String uuid) {
        return new PlayerEvent("event", System.currentTimeMillis(), "player_quit",
            new Payload(uuid, null, null));
    }
}
