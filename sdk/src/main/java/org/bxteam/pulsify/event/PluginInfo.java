package org.bxteam.pulsify.event;

/**
 * One installed plugin in a {@link Heartbeat}.
 *
 * @param name    plugin name
 * @param version plugin version
 * @param enabled whether the plugin is currently enabled
 */
public record PluginInfo(String name, String version, boolean enabled) {}
