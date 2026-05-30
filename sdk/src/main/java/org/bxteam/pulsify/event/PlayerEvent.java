package org.bxteam.pulsify.event;

/**
 * Wire payload for a player join/quit. Serialized with {@code type: "event"}; the {@code event}
 * field carries the specific action ({@code "player_join"} or {@code "player_quit"}). The
 * payload's UUID and IP are scrubbed of personal data server-side at ingest.
 *
 * @param type      event discriminator, always {@code "event"}
 * @param timestamp event time in epoch milliseconds
 * @param event     specific action: {@code "player_join"} or {@code "player_quit"}
 * @param payload   player details
 */
public record PlayerEvent(
    String type,
    long timestamp,
    String event,
    Payload payload
) {
    /**
     * Player details. Fields irrelevant to the action are {@code null} (e.g. a quit carries
     * only the UUID) and omitted from the JSON.
     *
     * @param playerUuid    player UUID
     * @param clientVersion client/protocol version, may be {@code null}
     * @param playerIp      player IP address, may be {@code null}
     */
    public record Payload(String playerUuid, String clientVersion, String playerIp) {}

    /** Builds a {@code player_join} event with the current timestamp. */
    public static PlayerEvent join(String uuid, String clientVersion, String playerIp) {
        return new PlayerEvent("event", System.currentTimeMillis(), "player_join",
            new Payload(uuid, clientVersion, playerIp));
    }

    /** Builds a {@code player_quit} event with the current timestamp. */
    public static PlayerEvent quit(String uuid) {
        return new PlayerEvent("event", System.currentTimeMillis(), "player_quit",
            new Payload(uuid, null, null));
    }
}
