package org.example.gateway.config;

import com.example.common.metrics.AppMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayMetricsFilter implements GlobalFilter, Ordered {

    private static final long SLOW_MS = 2000;

    private final AppMetrics appMetrics;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }
        long started = System.nanoTime();
        return chain.filter(exchange).doFinally(signal -> {
            long elapsedMs = Duration.ofNanos(System.nanoTime() - started).toMillis();
            HttpStatusCode status = exchange.getResponse().getStatusCode();
            int code = status != null ? status.value() : 502;
            appMetrics.shlyuzVremya(Duration.ofMillis(elapsedMs));
            if (code >= 400) {
                appMetrics.shlyuzOshibka(code);
                log.warn("[Шлюз] Ошибка запроса {} {} код {} за {} мс",
                        exchange.getRequest().getMethod(), path, code, elapsedMs);
            }
            if (elapsedMs >= SLOW_MS) {
                appMetrics.shlyuzMedlenno();
                log.warn("[Шлюз] Медленный запрос {} {} — {} мс",
                        exchange.getRequest().getMethod(), path, elapsedMs);
            }
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
