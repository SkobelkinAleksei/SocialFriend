package org.example.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BannedAccountGuard {

    private static final long ACTIVE_TTL_MS = 3_000;
    private static final long BLOCKED_TTL_MS = 30_000;

    private final WebClient webClient;
    private final ConcurrentHashMap<Long, Cached> cache = new ConcurrentHashMap<>();

    public BannedAccountGuard(
            @Value("${app.services.user}") String userUrl,
            @Value("${app.internal.api-key}") String apiKey
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(userUrl)
                .defaultHeader("X-Internal-Key", apiKey)
                .build();
    }

    public Mono<Boolean> isBlocked(long userId) {
        Cached cached = cache.get(userId);
        long now = System.currentTimeMillis();
        if (cached != null && cached.expiresAt > now) {
            return Mono.just(cached.blocked);
        }
        return webClient.get()
                .uri("/api/v1/internal/users/{id}/account-status", userId)
                .retrieve()
                .bodyToMono(StatusResponse.class)
                .timeout(Duration.ofMillis(400))
                .map(body -> {
                    String status = body == null || body.status() == null ? "ACTIVE" : body.status();
                    boolean blocked = "BANNED".equalsIgnoreCase(status) || "DELETED".equalsIgnoreCase(status);
                    cache.put(userId, new Cached(blocked, now + (blocked ? BLOCKED_TTL_MS : ACTIVE_TTL_MS)));
                    return blocked;
                })
                .onErrorReturn(false);
    }

    private record Cached(boolean blocked, long expiresAt) {}

    private record StatusResponse(String status) {}
}
