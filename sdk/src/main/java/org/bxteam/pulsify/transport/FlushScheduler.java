package org.bxteam.pulsify.transport;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Drives delivery of buffered events on a single daemon thread. It flushes on a fixed interval
 * and on demand when a batch fills up, so plugin threads never block on the network. Delivery
 * is serialized onto this one thread, which keeps ordering and backoff handling simple.
 */
public final class FlushScheduler {
    private final ScheduledExecutorService executor;
    private final EventQueue queue;
    private final HttpTransport transport;

    /**
     * @param queue         buffer to drain
     * @param transport     transport batches are sent through
     * @param flushInterval how often the periodic flush runs
     */
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

    /** Triggers an off-thread flush when the queue has filled a batch; a no-op otherwise. */
    public void checkAndFlushIfFull() {
        if (queue.isFull()) {
            executor.execute(this::flush);
        }
    }

    /**
     * Drains and sends batches until the queue is empty or a retryable failure opens a backoff
     * window. Does nothing while already backing off. Runs on the scheduler thread, but is safe
     * to invoke directly (e.g. from {@link org.bxteam.pulsify.StatClient#flush()}).
     */
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

    /** Performs a final flush, then shuts the executor down, waiting up to 30s for it to drain. */
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
