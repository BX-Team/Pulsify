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
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.logging.Logger;

public final class StatClient implements Closeable {
    private final EventQueue queue;
    private final HttpTransport transport;
    private final FlushScheduler scheduler;
    private final Set<String> ignoredPlugins;
    private final String serverVersion;
    private final String serverSoftware;
    private final Function<String, String> pluginVersionResolver;
    private final double sampleRate;
    private ErrorCollector errorCollector;

    private StatClient(Builder b) {
        Dsn dsn = Dsn.parse(b.dsn);
        this.queue = new EventQueue(b.maxBatchSize, b.maxQueueSize);
        this.transport = new HttpTransport(dsn.ingestUrl(), dsn.token(), queue, b.logger);
        this.scheduler = new FlushScheduler(queue, transport, b.flushInterval);
        this.ignoredPlugins = Set.copyOf(b.ignoredPlugins);
        this.serverVersion = b.serverVersion;
        this.serverSoftware = b.serverSoftware;
        this.pluginVersionResolver = b.pluginVersionResolver;
        this.sampleRate = b.sampleRate;
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
        enqueueSampled(new MetricEvent(name, value, labels));
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
        // Errors are never sampled — full fidelity is the whole point of the product.
        enqueue(new ErrorEvent(plugin, safeMessage, stacktrace != null ? stacktrace : "", level,
            serverVersion, serverSoftware, resolvePluginVersion(plugin)));
    }

    private String resolvePluginVersion(String plugin) {
        if (pluginVersionResolver == null || plugin == null) return null;
        try {
            return pluginVersionResolver.apply(plugin);
        } catch (Exception e) {
            return null;
        }
    }

    public void heartbeat(ServerInfo server, List<PluginInfo> plugins) {
        enqueue(new Heartbeat(server, plugins));
    }

    public void playerJoin(String uuid, String clientVersion, String playerIp) {
        enqueueSampled(PlayerEvent.join(uuid, clientVersion, playerIp));
    }

    public void playerQuit(String uuid) {
        enqueueSampled(PlayerEvent.quit(uuid));
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

    /**
     * Enqueues a high-volume event subject to client-side sampling. With a sample rate
     * below 1.0 a fraction of these events is dropped before they ever hit the queue,
     * keeping the network/storage cost bounded on busy servers.
     */
    private void enqueueSampled(Object event) {
        if (sampleRate < 1.0 && ThreadLocalRandom.current().nextDouble() >= sampleRate) return;
        enqueue(event);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String dsn;
        private Duration flushInterval = Duration.ofMinutes(5);
        private int maxBatchSize = 100;
        private int maxQueueSize = 10_000;
        private boolean autoCollectErrors = false;
        private Logger logger;
        private final Set<String> ignoredPlugins = new HashSet<>();
        private String serverVersion;
        private String serverSoftware;
        private Function<String, String> pluginVersionResolver;
        private double sampleRate = 1.0;

        public Builder dsn(String dsn) { this.dsn = dsn; return this; }
        public Builder flushInterval(Duration d) { this.flushInterval = d; return this; }
        public Builder maxBatchSize(int n) { this.maxBatchSize = n; return this; }
        public Builder maxQueueSize(int n) { this.maxQueueSize = n; return this; }
        public Builder autoCollectErrors(boolean v) { this.autoCollectErrors = v; return this; }
        public Builder logger(Logger l) { this.logger = l; return this; }

        /** Minecraft version stamped onto every captured error (e.g. "1.21.4"). */
        public Builder serverVersion(String v) { this.serverVersion = v; return this; }

        /** Server software stamped onto every captured error (e.g. "Paper"). */
        public Builder serverSoftware(String v) { this.serverSoftware = v; return this; }

        /** Resolves a plugin name to its version, stamped onto errors tagged with that plugin. */
        public Builder pluginVersionResolver(Function<String, String> r) { this.pluginVersionResolver = r; return this; }

        /** Fraction (0.0–1.0) of high-volume events (player events, metrics) to keep. Errors are never sampled. */
        public Builder sampleRate(double r) { this.sampleRate = Math.max(0.0, Math.min(1.0, r)); return this; }

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
