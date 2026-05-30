package org.bxteam.pulsify.transport;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bounded, thread-safe in-memory buffer of pending events. Producers (plugin threads) enqueue;
 * the flush thread drains in batches. When the buffer is full the oldest events are dropped to
 * cap memory use under sustained back-pressure — losing the stalest data is preferable to an
 * unbounded queue or blocking the hot path.
 */
public final class EventQueue {
    private final Queue<Object> queue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger size = new AtomicInteger(0);
    private final AtomicLong dropped = new AtomicLong(0);
    private final int maxBatchSize;
    private final int maxQueueSize;

    /**
     * @param maxBatchSize max events returned by a single {@link #drain()}; also the {@link #isFull()} threshold
     * @param maxQueueSize max events buffered before the oldest are dropped
     */
    public EventQueue(int maxBatchSize, int maxQueueSize) {
        this.maxBatchSize = maxBatchSize;
        this.maxQueueSize = maxQueueSize;
    }

    /** Adds an event, evicting the oldest events first if the buffer is at capacity. */
    public void enqueue(Object event) {
        while (size.get() >= maxQueueSize) {
            if (queue.poll() == null) break;
            size.decrementAndGet();
            dropped.incrementAndGet();
        }
        queue.add(event);
        size.incrementAndGet();
    }

    /** Removes and returns up to {@code maxBatchSize} events in FIFO order; empty when nothing is buffered. */
    public List<Object> drain() {
        List<Object> batch = new ArrayList<>(Math.min(size.get(), maxBatchSize));
        Object item;
        int count = 0;
        while (count < maxBatchSize && (item = queue.poll()) != null) {
            batch.add(item);
            size.decrementAndGet();
            count++;
        }
        return batch;
    }

    /** Current number of buffered events. */
    public int size() {
        return size.get();
    }

    /** Total events discarded because the queue was full. */
    public long droppedCount() {
        return dropped.get();
    }

    /** True once at least {@code maxBatchSize} events are buffered — the cue for an immediate flush. */
    public boolean isFull() {
        return size.get() >= maxBatchSize;
    }
}
