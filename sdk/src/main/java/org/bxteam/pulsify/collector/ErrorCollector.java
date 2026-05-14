package org.bxteam.pulsify.collector;

import org.bxteam.pulsify.StatClient;

public interface ErrorCollector {
    void install(StatClient client, String pluginName);

    void uninstall();
}
