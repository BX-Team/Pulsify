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

/**
 * Entry point of the Pulsify SDK. A single long-lived client owns the event queue, the HTTP
 * transport and the background flush scheduler, and exposes the methods plugins call to report
 * errors, metrics, heartbeats and player activity.
 *
 * <p>Events are buffered in memory and flushed in batches — periodically on a background daemon
 * thread and immediately whenever a batch fills up — so calls on the hot path never block on the
 * network. Build one with {@link #builder()} and {@link #close() close} it on shutdown to flush
 * any pending events.
 *
 * <p>Instances are thread-safe; share one across the whole plugin.
 */
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

    /** Records a custom metric with no labels. Equivalent to {@code metric(name, value, null)}. */
    public void metric(String name, double value) {
        metric(name, value, null);
    }

    /**
     * Records a custom numeric metric. Metrics are high-volume events and subject to the
     * configured {@link Builder#sampleRate(double) sample rate}.
     *
     * @param name   metric name
     * @param value  measured value
     * @param labels optional key/value dimensions, may be {@code null}
     */
    public void metric(String name, double value, Map<String, String> labels) {
        enqueueSampled(new MetricEvent(name, value, labels));
    }

    /** Captures a throwable at {@link ErrorLevel#ERROR}. See {@link #error(String, Throwable, ErrorLevel)}. */
    public void error(String plugin, Throwable t) {
        error(plugin, t, ErrorLevel.ERROR);
    }

    /**
     * Captures a throwable as an error event, recording its message and full stack trace.
     * Falls back to the exception's class name when it carries no message.
     *
     * @param plugin plugin the error is attributed to (matched against the ignore list)
     * @param t      the throwable to capture
     * @param level  severity to record it at
     */
    public void error(String plugin, Throwable t, ErrorLevel level) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        String msg = t.getMessage();
        if (msg == null || msg.isEmpty()) msg = t.getClass().getName();
        captureError(plugin, msg, sw.toString(), level);
    }

    /** Captures a message-only error with no stack trace. See {@link #captureError}. */
    public void error(String plugin, String message, ErrorLevel level) {
        captureError(plugin, message, null, level);
    }

    /**
     * Captures an error event from its raw parts. The lowest-level error entry point — the
     * other {@code error(...)} overloads funnel into it.
     *
     * <p>Events for plugins on the ignore list are dropped. Unlike metrics and player events,
     * errors are never sampled — full fidelity is the point of the product. Player-identifying
     * data (UUIDs, IPs, emails) in the message or stack trace is scrubbed server-side at ingest,
     * so callers need not redact it themselves.
     *
     * @param plugin     plugin the error is attributed to
     * @param message    error message; replaced with a placeholder when null or empty
     * @param stacktrace stack trace text, may be {@code null}
     * @param level      severity to record it at
     */
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

    /**
     * Sends a heartbeat snapshot of server state and the installed plugin list. Typically
     * invoked on a fixed schedule (e.g. once a minute) to drive the live dashboard.
     */
    public void heartbeat(ServerInfo server, List<PluginInfo> plugins) {
        enqueue(new Heartbeat(server, plugins));
    }

    /**
     * Reports a player joining. High-volume event, subject to the configured sample rate.
     *
     * @param uuid          player UUID
     * @param clientVersion client/protocol version, may be {@code null}
     * @param playerIp      player IP address, may be {@code null}; scrubbed server-side
     */
    public void playerJoin(String uuid, String clientVersion, String playerIp) {
        enqueueSampled(PlayerEvent.join(uuid, clientVersion, playerIp));
    }

    /** Reports a player leaving. High-volume event, subject to the configured sample rate. */
    public void playerQuit(String uuid) {
        enqueueSampled(PlayerEvent.quit(uuid));
    }

    /**
     * Checks connectivity and credentials against the ingest host without enqueuing anything.
     *
     * @return a future completing with {@code true} when the server answers 200, {@code false}
     *         on any non-200 response or network error
     */
    public CompletableFuture<Boolean> pingAsync() {
        return transport.ping();
    }

    /**
     * Name of the logger the transport reports its own send failures to. Consumed by the auto
     * error collector so it can skip those records and avoid re-capturing Pulsify's own warnings
     * into an endless feedback loop. Public only because the collectors live in a sub-package.
     */
    public String transportLoggerName() {
        return transport.loggerName();
    }

    /** Flushes all buffered events now, draining the queue on the calling thread. */
    public void flush() {
        scheduler.flush();
    }

    /**
     * Uninstalls the auto error collector (if any), performs a final flush and shuts down the
     * background scheduler. Call on plugin disable so pending events are not lost.
     */
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

    /** Creates a new builder. {@link Builder#dsn(String)} is the only required setting. */
    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder for {@link StatClient}. All settings are optional except the DSN. */
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

        /** Required. The Pulsify connection string; see {@link Dsn}. */
        public Builder dsn(String dsn) { this.dsn = dsn; return this; }

        /** How often the background scheduler flushes buffered events. Default 5 minutes. */
        public Builder flushInterval(Duration d) { this.flushInterval = d; return this; }

        /** Max events per HTTP batch; also the threshold that triggers an immediate flush. Default 100. */
        public Builder maxBatchSize(int n) { this.maxBatchSize = n; return this; }

        /** Max events buffered in memory; the oldest are dropped once full. Default 10,000. */
        public Builder maxQueueSize(int n) { this.maxQueueSize = n; return this; }

        /** When true, hooks the server's logging framework to capture warnings/errors automatically. Default false. */
        public Builder autoCollectErrors(boolean v) { this.autoCollectErrors = v; return this; }

        /** Logger the transport reports its own send failures to. Defaults to a {@code "Pulsify"} JUL logger. */
        public Builder logger(Logger l) { this.logger = l; return this; }

        /** Minecraft version stamped onto every captured error (e.g. "1.21.4"). */
        public Builder serverVersion(String v) { this.serverVersion = v; return this; }

        /** Server software stamped onto every captured error (e.g. "Paper"). */
        public Builder serverSoftware(String v) { this.serverSoftware = v; return this; }

        /** Resolves a plugin name to its version, stamped onto errors tagged with that plugin. */
        public Builder pluginVersionResolver(Function<String, String> r) { this.pluginVersionResolver = r; return this; }

        /** Fraction (0.0–1.0) of high-volume events (player events, metrics) to keep. Errors are never sampled. */
        public Builder sampleRate(double r) { this.sampleRate = Math.max(0.0, Math.min(1.0, r)); return this; }

        /** Adds a plugin whose errors are dropped before enqueuing. Case-insensitive; blanks ignored. */
        public Builder ignorePlugin(String name) {
            if (name != null && !name.isBlank()) ignoredPlugins.add(name.toLowerCase(Locale.ROOT));
            return this;
        }

        /** Adds several plugins to the ignore list at once. See {@link #ignorePlugin(String)}. */
        public Builder ignorePlugins(Collection<String> names) {
            if (names != null) names.forEach(this::ignorePlugin);
            return this;
        }

        /**
         * Builds the client and starts its background scheduler.
         *
         * @throws IllegalStateException if no DSN was set
         */
        public StatClient build() {
            if (dsn == null || dsn.isBlank()) throw new IllegalStateException("DSN is required");
            return new StatClient(this);
        }
    }
}
