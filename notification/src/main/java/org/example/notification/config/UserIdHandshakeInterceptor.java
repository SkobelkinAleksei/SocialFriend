package org.example.notification.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Identity только с HTTP handshake (X-User-Id, который выставил gateway из JWT).
 * STOMP-заголовки и query userId клиент контролировать не должен.
 */
public class UserIdHandshakeInterceptor implements HandshakeInterceptor {

    public static final String USER_ID_ATTR = "handshake.userId";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String userId = request.getHeaders().getFirst("X-User-Id");
        if (userId == null || userId.isBlank()) {
            return false;
        }
        String trimmed = userId.trim();
        try {
            Long.parseLong(trimmed);
        } catch (NumberFormatException e) {
            return false;
        }
        attributes.put(USER_ID_ATTR, trimmed);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }
}
