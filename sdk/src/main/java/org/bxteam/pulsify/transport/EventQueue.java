package org.bxteam.pulsify.transport;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class EventQueue {
    private final Queue<Object> queue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger size = new AtomicInteger(0);
    private final int maxBatchSize;

    public EventQueue(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }

    public void enqueue(Object event) {
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

    public boolean isFull() {
        return size.get() >= maxBatchSize;
    }
}
