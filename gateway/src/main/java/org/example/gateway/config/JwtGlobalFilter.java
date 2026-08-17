package org.example.gateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

@Component
public class JwtGlobalFilter implements GlobalFilter, Ordered {

    private final JwtUtils jwtUtils;
    private final BannedAccountGuard bannedAccountGuard;

    /** Точное начало пути (не contains) — чтобы нельзя было «протащить» open-path в чужом URL. */
    private final List<String> openEndpointPrefixes = List.of(
            "/api/v1/social/registration/",
            "/api/v1/social/auth/login",
            "/api/v1/social/auth/refresh",
            "/api/v1/social/auth/logout",
            "/api/v1/geo/suggest"
    );

    public JwtGlobalFilter(JwtUtils jwtUtils) {
        this(jwtUtils, null);
    }

    @Autowired
    public JwtGlobalFilter(JwtUtils jwtUtils, BannedAccountGuard bannedAccountGuard) {
        this.jwtUtils = jwtUtils;
        this.bannedAccountGuard = bannedAccountGuard;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
            return chain.filter(stripClientUserId(exchange));
        }

        String path = exchange.getRequest().getURI().getPath();
        if (isOpenEndpoint(path) || isPublicMedia(exchange)) {
            return chain.filter(stripClientUserId(exchange));
        }

        String jwt = extractJwt(exchange.getRequest(), path);
        if (jwt == null || !jwtUtils.isTokenValid(jwt)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        long userId = jwtUtils.extractUserId(jwt);
        String role = jwtUtils.extractRole(jwt);
        if ("ADMIN".equals(role) && !isAdminAllowedPath(path, exchange.getRequest().getMethod())) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
        builder.headers(headers -> {
            headers.remove("X-User-Id");
            headers.set("X-User-Id", String.valueOf(userId));
            headers.remove("X-Platform-Role");
            headers.set("X-Platform-Role", role);
        });
        if (path.startsWith("/ws/")) {
            URI uri = UriComponentsBuilder.fromUri(exchange.getRequest().getURI())
                    .replaceQueryParam("userId")
                    .replaceQueryParam("token")
                    .build(true)
                    .toUri();
            builder.uri(uri);
        }
        ServerWebExchange next = exchange.mutate().request(builder.build()).build();
        if ("ADMIN".equals(role) || bannedAccountGuard == null) {
            return chain.filter(next);
        }
        return bannedAccountGuard.isBlocked(userId).flatMap(blocked -> {
            if (!blocked) {
                return chain.filter(next);
            }
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        });
    }

    private static String extractJwt(ServerHttpRequest request, String path) {
        if (path.startsWith("/ws/")) {
            return request.getQueryParams().getFirst("token");
        }
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private static ServerWebExchange stripClientUserId(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> headers.remove("X-User-Id"))
                .build();
        return exchange.mutate().request(request).build();
    }

    private boolean isOpenEndpoint(String path) {
        return openEndpointPrefixes.stream().anyMatch(path::startsWith);
    }

    private static boolean isPublicMedia(ServerWebExchange exchange) {
        if (!HttpMethod.GET.equals(exchange.getRequest().getMethod())) {
            return false;
        }
        String path = exchange.getRequest().getURI().getPath();
        return path.startsWith("/api/v1/social/events/media/")
                || path.startsWith("/api/v1/social/chats/media/")
                || path.startsWith("/api/v1/social/posts/media/")
                || path.startsWith("/api/v1/social/users/media/");
    }

    private static boolean isAdminAllowedPath(String path, HttpMethod method) {
        if (path.startsWith("/api/v1/social/admin/")) {
            return true;
        }
        if (path.startsWith("/api/v1/social/auth/")) {
            return true;
        }
        if (HttpMethod.GET.equals(method) && "/api/v1/social/users/me".equals(path)) {
            return true;
        }
        return HttpMethod.GET.equals(method) && (
                path.startsWith("/api/v1/social/events/media/")
                        || path.startsWith("/api/v1/social/chats/media/")
                        || path.startsWith("/api/v1/social/posts/media/")
                        || path.startsWith("/api/v1/social/users/media/")
        );
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
