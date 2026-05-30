package org.bxteam.pulsify.collector;

import org.bxteam.pulsify.StatClient;

/**
 * Bridges the host's logging framework to a {@link StatClient}, turning logged warnings and
 * errors into captured error events automatically. The client picks the implementation that
 * matches the runtime's logging backend ({@link Log4j2Appender} when Log4j2 is present, else
 * {@link JulHandler}).
 */
public interface ErrorCollector {
    /**
     * Hooks this collector into the logging framework so subsequent log records flow to the client.
     *
     * @param client     the client captured records are forwarded to
     * @param pluginName fallback plugin name for records whose logger name is blank
     */
    void install(StatClient client, String pluginName);

    /** Unhooks this collector from the logging framework. Safe to call even if not installed. */
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
