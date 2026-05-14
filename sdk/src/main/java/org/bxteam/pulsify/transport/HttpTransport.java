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
import java.util.concurrent.TimeUnit;

public final class HttpTransport {
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String ingestUrl;
    private final String bearerToken;
    private final EventQueue queue;

    public HttpTransport(String ingestUrl, String token, EventQueue queue) {
        this.ingestUrl = ingestUrl;
        this.bearerToken = token;
        this.queue = queue;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

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

    public void send(List<Object> events) {
        if (events.isEmpty()) return;
        try {
            String body = mapper.writeValueAsString(events);
            sendWithRetry(body, events, 0);
        } catch (Exception e) {
            System.err.println("[Pulsify] Serialization failed: " + e.getMessage());
        }
    }

    private void sendWithRetry(String body, List<Object> events, int attempt) {
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

            if (status == 429) {
                events.forEach(queue::enqueue);
                System.err.println("[Pulsify] Rate limited (429), events re-queued.");
            } else if (status >= 500 && attempt == 0) {
                TimeUnit.SECONDS.sleep(5);
                sendWithRetry(body, events, 1);
            } else if (status >= 400) {
                System.err.println("[Pulsify] Request rejected (" + status + "): " + response.body());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("[Pulsify] HTTP error: " + e.getMessage());
        }
    }
}
