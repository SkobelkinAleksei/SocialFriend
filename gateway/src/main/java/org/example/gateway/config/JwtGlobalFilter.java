package org.example.gateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class JwtGlobalFilter implements GlobalFilter, Ordered {

    private final JwtUtils jwtUtils;
    private final List<String> openEndpoints = List.of(
            "/api/v1/social/registration/signUp",
            "/api/v1/social/auth/login"
    );

    public JwtGlobalFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // Проверяем, есть ли путь в списке открытых
        boolean isApiOpen = openEndpoints.stream()
                .anyMatch(path::contains);

        if (isApiOpen) {
            return chain.filter(exchange);
        }

        String authHeader = exchange
                .getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String jwt = authHeader.substring(7);
        if (!jwtUtils.isTokenValid(jwt)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        long userId = jwtUtils.extractUserId(jwt);

        // 1. Создаём модифицированный request с заголовком X-User-Id
        ServerHttpRequest modifiedRequest = exchange
                .getRequest()
                .mutate()
                .header("X-User-Id", String.valueOf(userId))
                .build();

        // 2. Создаём модифицированный exchange с этим request
        ServerWebExchange modifiedExchange = exchange.mutate()
                .request(modifiedRequest)
                .build();

        // 3. Передаём модифицированный exchange дальше
        return chain.filter(modifiedExchange);
    }


    @Override
    public int getOrder() {
        return -1; // самая высокая приоритетность
    }
}