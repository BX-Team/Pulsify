package org.bxteam.pulsify.event;

import java.util.Map;

public record MetricEvent(
    String type,
    long timestamp,
    String name,
    double value,
    Map<String, String> labels
) {
    public MetricEvent(String name, double value, Map<String, String> labels) {
        this("metric", System.currentTimeMillis(), name, value, labels);
    }
}
