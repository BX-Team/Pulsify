package org.bxteam.pulsify.collector;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.bxteam.pulsify.ErrorLevel;
import org.bxteam.pulsify.StatClient;

public final class Log4j2Appender extends AbstractAppender implements ErrorCollector {
    private StatClient client;
    private String defaultPluginName;
    private LoggerContext context;

    public Log4j2Appender() {
        super("PulsifyAppender", null, null, true, Property.EMPTY_ARRAY);
    }

    @Override
    public void install(StatClient client, String pluginName) {
        this.client = client;
        this.defaultPluginName = pluginName;
        this.context = (LoggerContext) LogManager.getContext(false);
        context.getConfiguration().addAppender(this);
        context.getConfiguration().getRootLogger().addAppender(this, Level.WARN, null);
        context.updateLoggers();
        start();
    }

    @Override
    public void uninstall() {
        stop();
        if (context != null) {
            context.getConfiguration().getRootLogger().removeAppender("PulsifyAppender");
            context.updateLoggers();
        }
    }

    @Override
    public void append(LogEvent event) {
        if (client == null) return;

        ErrorLevel level = mapLevel(event.getLevel());
        if (level == null) return;

        String plugin = event.getLoggerName();
        if (plugin == null || plugin.isBlank())
            plugin = defaultPluginName != null ? defaultPluginName : "server";

        if (event.getThrown() != null) {
            client.error(plugin, event.getThrown(), level);
        } else {
            String message = event.getMessage() != null ? event.getMessage().getFormattedMessage() : null;
            client.error(plugin, message, level);
        }
    }

    private ErrorLevel mapLevel(Level level) {
        if (level == Level.WARN) return ErrorLevel.WARNING;
        if (level == Level.ERROR) return ErrorLevel.ERROR;
        if (level == Level.FATAL) return ErrorLevel.FATAL;
        return null;
    }
}
