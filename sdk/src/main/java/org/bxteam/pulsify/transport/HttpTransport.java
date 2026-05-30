package org.bxteam.pulsify.transport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Serializes event batches to JSON and POSTs them to the ingest endpoint. Encapsulates the
 * delivery policy: transient failures (HTTP 429/5xx, network errors) re-queue the batch and
 * open an exponential backoff window so the next flush waits instead of hammering the API;
 * permanent failures (other 4xx, unserializable payloads) drop the batch. JSON is rendered in
 * snake_case with nulls omitted to match the server schema.
 */
public final class HttpTransport {
    private static final long BASE_BACKOFF_MS = 5_000;
    private static final long MAX_BACKOFF_MS = 120_000;

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String ingestUrl;
    private final String bearerToken;
    private final EventQueue queue;
    private final Logger logger;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private volatile long backoffUntilMs = 0;

    /**
     * @param ingestUrl resolved ingest endpoint from the {@link org.bxteam.pulsify.Dsn}
     * @param token     bearer token sent on every request
     * @param queue     buffer that retryable batches are re-queued into
     * @param logger    where send failures are reported; a {@code "Pulsify"} logger when null
     */
    public HttpTransport(String ingestUrl, String token, EventQueue queue, Logger logger) {
        this.ingestUrl = ingestUrl;
        this.bearerToken = token;
        this.queue = queue;
        this.logger = logger != null ? logger : Logger.getLogger("Pulsify");
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * Probes the ping endpoint to verify connectivity and credentials without enqueuing data.
     *
     * @return a future completing with {@code true} on HTTP 200, {@code false} otherwise
     */
    public CompletableFuture<Boolean> ping() {
        String pingUrl = ingestUrl.replace("/api/v1/e/", "/api/v1/ping/");
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(pingUrl))
                    .header("Authorization", "Bearer " + bearerToken)
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
                HttpResponse<Void> res = httpClient.send(req, HttpResponse.BodyHandlers.discarding());
                return res.statusCode() == 200;
            } catch (Exception e) {
                return false;
            }
        });
    }

    /** True while inside a backoff window opened by recent retryable failures. */
    public boolean isBackingOff() {
        return System.currentTimeMillis() < backoffUntilMs;
    }

    /** Name of the logger this transport reports failures to — lets the error collector skip them. */
    public String loggerName() {
        return logger.getName();
    }

    /**
     * Sends one batch synchronously.
     *
     * @return {@code true} if the caller may keep draining the queue — the batch was
     *         delivered, or permanently rejected and dropped. {@code false} on a
     *         retryable failure: the events were re-queued and a backoff window was
     *         opened, so the caller must stop draining until it expires.
     */
    public boolean send(List<Object> events) {
        if (events.isEmpty()) return true;

        String body;
        try {
            body = mapper.writeValueAsString(events);
        } catch (Exception e) {
            // A malformed payload will never serialize, so retrying is pointless — drop it.
            logger.severe("[Pulsify] Serialization failed, dropping " + events.size() + " events: " + e.getMessage());
            return true;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ingestUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + bearerToken)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(30))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status >= 200 && status < 300) {
                onSuccess();
                return true;
            }

            if (status == 429 || status >= 500) {
                // Transient (rate limited / server error): keep the data and back off
                // so we don't hammer the API on the next flush tick.
                requeue(events);
                long delay = backoff();
                logger.warning("[Pulsify] " + (status == 429 ? "Rate limited" : "Server error " + status)
                    + ", re-queued " + events.size() + " events; backing off " + (delay / 1000) + "s.");
                return false;
            }

            // Other 4xx: client-side issue (bad DSN, rejected payload). Retrying won't
            // help so drop the batch, but the server is reachable — clear any backoff.
            onSuccess();
            logger.warning("[Pulsify] Request rejected (" + status + "), dropping " + events.size()
                + " events: " + response.body());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            requeue(events);
            return false;
        } catch (Exception e) {
            // Network error / timeout: don't lose the data, back off and retry next flush.
            requeue(events);
            long delay = backoff();
            logger.warning("[Pulsify] HTTP error, re-queued " + events.size() + " events; backing off "
                + (delay / 1000) + "s: " + e.getMessage());
            return false;
        }
    }

    private void requeue(List<Object> events) {
        events.forEach(queue::enqueue);
    }

    private void onSuccess() {
        consecutiveFailures.set(0);
        backoffUntilMs = 0;
    }

    private long backoff() {
        int failures = consecutiveFailures.incrementAndGet();
        long delay = Math.min(BASE_BACKOFF_MS * (1L << Math.min(failures - 1, 5)), MAX_BACKOFF_MS);
        backoffUntilMs = System.currentTimeMillis() + delay;
        return delay;
    }
}
