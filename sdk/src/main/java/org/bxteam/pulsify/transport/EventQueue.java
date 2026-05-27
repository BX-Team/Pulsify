package org.bxteam.pulsify.transport;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class EventQueue {
    private final Queue<Object> queue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger size = new AtomicInteger(0);
    private final AtomicLong dropped = new AtomicLong(0);
    private final int maxBatchSize;
    private final int maxQueueSize;

    public EventQueue(int maxBatchSize, int maxQueueSize) {
        this.maxBatchSize = maxBatchSize;
        this.maxQueueSize = maxQueueSize;
    }

    public void enqueue(Object event) {
        while (size.get() >= maxQueueSize) {
            if (queue.poll() == null) break;
            size.decrementAndGet();
            dropped.incrementAndGet();
        }
        queue.add(event);
        size.incrementAndGet();
    }

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

    public int size() {
        return size.get();
    }

    /** Total events discarded because the queue was full. */
    public long droppedCount() {
        return dropped.get();
    }

    public boolean isFull() {
        return size.get() >= maxBatchSize;
    }
}
