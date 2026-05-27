package org.bxteam.pulsify.transport;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class FlushScheduler {
    private final ScheduledExecutorService executor;
    private final EventQueue queue;
    private final HttpTransport transport;

    public FlushScheduler(EventQueue queue, HttpTransport transport, Duration flushInterval) {
        this.queue = queue;
        this.transport = transport;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "pulsify-flush");
            t.setDaemon(true);
            return t;
        });
        long ms = flushInterval.toMillis();
        executor.scheduleAtFixedRate(this::flush, ms, ms, TimeUnit.MILLISECONDS);
    }

    public void checkAndFlushIfFull() {
        if (queue.isFull()) {
            executor.execute(this::flush);
        }
    }

    public void flush() {
        // Skip while backing off after recent failures — re-queued events would
        // otherwise be drained and re-sent immediately, hammering the API.
        if (transport.isBackingOff()) return;

        List<Object> batch;
        while (!(batch = queue.drain()).isEmpty()) {
            // Stop on a retryable failure: the batch was re-queued and a backoff
            // window opened, so draining again now would just re-send it.
            if (!transport.send(batch)) break;
        }
    }

    public void shutdown() {
        try {
            flush();
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
