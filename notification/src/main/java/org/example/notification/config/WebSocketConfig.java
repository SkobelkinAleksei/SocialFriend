package org.example.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.Map;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/notifications")
                .addInterceptors(new UserIdHandshakeInterceptor())
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) {
                    return message;
                }

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String userId = handshakeUserId(accessor);
                    if (userId == null) {
                        throw new IllegalArgumentException("WebSocket CONNECT без handshake userId запрещён");
                    }
                    accessor.setUser(() -> userId);
                    log.info("[WebSocket-Notifications] CONNECT userId={}", userId);
                }

                if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    Principal principal = accessor.getUser();
                    if (principal == null || principal.getName() == null) {
                        throw new IllegalArgumentException("SUBSCRIBE без авторизации запрещён");
                    }
                    String dest = accessor.getDestination();
                    if (dest == null) {
                        return message;
                    }

                    String name = principal.getName();
                    if (dest.startsWith("/user/") && !dest.startsWith("/user/queue")) {
                        if (!dest.startsWith("/user/" + name + "/") && !dest.equals("/user/" + name)) {
                            throw new IllegalArgumentException("Запрещена подписка на чужую user-очередь");
                        }
                    }

                    if (dest.startsWith("/topic/notifications-")) {
                        String topicUserId = dest.substring("/topic/notifications-".length());
                        int slash = topicUserId.indexOf('/');
                        if (slash >= 0) {
                            topicUserId = topicUserId.substring(0, slash);
                        }
                        if (!name.equals(topicUserId)) {
                            throw new IllegalArgumentException("Запрещена подписка на чужие уведомления");
                        }
                    }
                }

                return message;
            }
        });
    }

    private static String handshakeUserId(StompHeaderAccessor accessor) {
        Map<String, Object> attrs = accessor.getSessionAttributes();
        if (attrs == null) {
            return null;
        }
        Object raw = attrs.get(UserIdHandshakeInterceptor.USER_ID_ATTR);
        if (raw == null) {
            return null;
        }
        String userId = String.valueOf(raw).trim();
        return userId.isBlank() ? null : userId;
    }
}
