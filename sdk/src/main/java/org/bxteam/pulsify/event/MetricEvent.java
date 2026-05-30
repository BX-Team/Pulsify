package org.bxteam.pulsify.event;

import java.util.Map;

/**
 * Wire payload for a custom metric. Serialized with {@code type: "metric"}; {@code labels}
 * is omitted from the JSON when null.
 *
 * @param type      event discriminator, always {@code "metric"}
 * @param timestamp record time in epoch milliseconds
 * @param name      metric name
 * @param value     measured value
 * @param labels    optional key/value dimensions, may be {@code null}
 */
public record MetricEvent(
    String type,
    long timestamp,
    String name,
    double value,
    Map<String, String> labels
) {
    /** Convenience constructor that stamps the {@code "metric"} type and current timestamp. */
    public MetricEvent(String name, double value, Map<String, String> labels) {
        this("metric", System.currentTimeMillis(), name, value, labels);
    }
}
