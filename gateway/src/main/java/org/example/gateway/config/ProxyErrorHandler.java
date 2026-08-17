package org.example.gateway.config;

import io.netty.resolver.dns.DnsNameResolverTimeoutException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProxyErrorHandler implements WebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (!isUpstreamUnreachable(ex) || exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        exchange.getResponse().setStatusCode(HttpStatus.BAD_GATEWAY);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = """
                {"title":"Bad Gateway","detail":"Не удалось связаться с сервисом. Попробуйте позже.","status":502}
                """.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private static boolean isUpstreamUnreachable(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof UnknownHostException
                    || current instanceof DnsNameResolverTimeoutException
                    || current instanceof ConnectException
                    || current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
