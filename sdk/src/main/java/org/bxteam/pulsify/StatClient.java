package org.bxteam.pulsify;

import org.bxteam.pulsify.collector.ErrorCollector;
import org.bxteam.pulsify.collector.JulHandler;
import org.bxteam.pulsify.collector.Log4j2Appender;
import org.bxteam.pulsify.event.ErrorEvent;
import org.bxteam.pulsify.event.Heartbeat;
import org.bxteam.pulsify.event.MetricEvent;
import org.bxteam.pulsify.event.PlayerEvent;
import org.bxteam.pulsify.event.PluginInfo;
import org.bxteam.pulsify.event.ServerInfo;
import org.bxteam.pulsify.transport.EventQueue;
import org.bxteam.pulsify.transport.FlushScheduler;
import org.bxteam.pulsify.transport.HttpTransport;

import java.io.Closeable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class StatClient implements Closeable {
    private final EventQueue queue;
    private final HttpTransport transport;
    private final FlushScheduler scheduler;
    private final Set<String> ignoredPlugins;
    private ErrorCollector errorCollector;

    private StatClient(Builder b) {
        Dsn dsn = Dsn.parse(b.dsn);
        this.queue = new EventQueue(b.maxBatchSize);
        this.transport = new HttpTransport(dsn.ingestUrl(), dsn.token(), queue);
        this.scheduler = new FlushScheduler(queue, transport, b.flushInterval, b.maxBatchSize);
        this.ignoredPlugins = Set.copyOf(b.ignoredPlugins);
        if (b.autoCollectErrors) {
            installErrorCollector("server");
        }
    }

    private void installErrorCollector(String pluginName) {
        ErrorCollector collector;
        try {
            Class.forName("org.apache.logging.log4j.core.LoggerContext");
            collector = new Log4j2Appender();
        } catch (ClassNotFoundException e) {
            collector = new JulHandler();
        }
        collector.install(this, pluginName);
        this.errorCollector = collector;
    }

    public void metric(String name, double value) {
        metric(name, value, null);
    }

    public void metric(String name, double value, Map<String, String> labels) {
        enqueue(new MetricEvent(name, value, labels));
    }

    public void error(String plugin, Throwable t) {
        error(plugin, t, ErrorLevel.ERROR);
    }

    public void error(String plugin, Throwable t, ErrorLevel level) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        String msg = t.getMessage();
        if (msg == null || msg.isEmpty()) msg = t.getClass().getName();
        captureError(plugin, msg, sw.toString(), level);
    }

    public void error(String plugin, String message, ErrorLevel level) {
        captureError(plugin, message, null, level);
    }

    public void captureError(String plugin, String message, String stacktrace, ErrorLevel level) {
        if (plugin != null && ignoredPlugins.contains(plugin.toLowerCase(Locale.ROOT))) return;
        String safeMessage = (message == null || message.isEmpty()) ? "(no message)" : message;
        enqueue(new ErrorEvent(plugin, safeMessage, stacktrace != null ? stacktrace : "", level));
    }

    public void heartbeat(ServerInfo server, List<PluginInfo> plugins) {
        enqueue(new Heartbeat(server, plugins));
    }

    public void playerJoin(String uuid, String clientVersion, String playerIp) {
        enqueue(PlayerEvent.join(uuid, clientVersion, playerIp));
    }

    public void playerQuit(String uuid) {
        enqueue(PlayerEvent.quit(uuid));
    }

    public CompletableFuture<Boolean> pingAsync() {
        return transport.ping();
    }

    public void flush() {
        scheduler.flush();
    }

    @Override
    public void close() {
        if (errorCollector != null) errorCollector.uninstall();
        scheduler.shutdown();
    }

    private void enqueue(Object event) {
        queue.enqueue(event);
        scheduler.checkAndFlushIfFull();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String dsn;
        private Duration flushInterval = Duration.ofMinutes(5);
        private int maxBatchSize = 100;
        private boolean autoCollectErrors = false;
        private final Set<String> ignoredPlugins = new HashSet<>();

        public Builder dsn(String dsn) { this.dsn = dsn; return this; }
        public Builder flushInterval(Duration d) { this.flushInterval = d; return this; }
        public Builder maxBatchSize(int n) { this.maxBatchSize = n; return this; }
        public Builder autoCollectErrors(boolean v) { this.autoCollectErrors = v; return this; }

        public Builder ignorePlugin(String name) {
            if (name != null && !name.isBlank()) ignoredPlugins.add(name.toLowerCase(Locale.ROOT));
            return this;
        }

        public Builder ignorePlugins(Collection<String> names) {
            if (names != null) names.forEach(this::ignorePlugin);
            return this;
        }

        public StatClient build() {
            if (dsn == null || dsn.isBlank()) throw new IllegalStateException("DSN is required");
            return new StatClient(this);
        }
    }
}
