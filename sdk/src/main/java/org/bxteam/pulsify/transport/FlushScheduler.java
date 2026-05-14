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
    private final int maxBatchSize;

    public FlushScheduler(EventQueue queue, HttpTransport transport, Duration flushInterval, int maxBatchSize) {
        this.queue = queue;
        this.transport = transport;
        this.maxBatchSize = maxBatchSize;
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
        List<Object> batch;
        while (!(batch = queue.drain()).isEmpty()) {
            transport.send(batch);
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
