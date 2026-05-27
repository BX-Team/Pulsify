package org.bxteam.pulsify.collector;

import org.bxteam.pulsify.StatClient;

public interface ErrorCollector {
    void install(StatClient client, String pluginName);

    void uninstall();

    /**
     * Whether a log record originated from Pulsify itself. Such records must be skipped:
     * the transport logs its own send failures as warnings, and capturing those as error
     * events would feed them back into the queue — an endless feedback loop.
     */
    default boolean isSelfLog(String loggerName) {
        return loggerName != null
            && (loggerName.equals("Pulsify") || loggerName.startsWith("org.bxteam.pulsify"));
    }
}
