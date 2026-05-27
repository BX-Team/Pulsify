package org.bxteam.pulsify.collector;

import org.bxteam.pulsify.ErrorLevel;
import org.bxteam.pulsify.StatClient;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public final class JulHandler extends Handler implements ErrorCollector {
    private static final Formatter FORMATTER = new SimpleFormatter();
    private StatClient client;
    private String defaultPluginName;

    @Override
    public void install(StatClient client, String pluginName) {
        this.client = client;
        this.defaultPluginName = pluginName;
        LogManager.getLogManager().getLogger("").addHandler(this);
    }

    @Override
    public void uninstall() {
        LogManager.getLogManager().getLogger("").removeHandler(this);
    }

    @Override
    public void publish(LogRecord record) {
        if (client == null || !isLoggable(record)) return;
        if (isSelfLog(record.getLoggerName())) return;

        ErrorLevel level = mapLevel(record.getLevel());
        if (level == null) return;

        String plugin = record.getLoggerName();
        if (plugin == null || plugin.isBlank())
            plugin = defaultPluginName != null ? defaultPluginName : "server";

        if (record.getThrown() != null) {
            client.error(plugin, record.getThrown(), level);
        } else {
            client.error(plugin, FORMATTER.formatMessage(record), level);
        }
    }

    private ErrorLevel mapLevel(Level level) {
        if (level == Level.WARNING) return ErrorLevel.WARNING;
        if (level == Level.SEVERE) return ErrorLevel.ERROR;
        return null;
    }

    @Override
    public boolean isLoggable(LogRecord record) {
        return record.getLevel().intValue() >= Level.WARNING.intValue();
    }

    @Override public void flush() {}
    @Override public void close() { uninstall(); }
}
