package org.example.chat.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chat.repository.ChatParticipantRepository;
import com.example.common.metrics.AppMetrics;
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
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ChatParticipantRepository chatParticipantRepository;
    private final AppMetrics appMetrics;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
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
                    appMetrics.chatWsPodklyuchenie();
                    log.info("[Чат] Подключение к WebSocket userId={}", userId);
                }

                if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                    appMetrics.chatWsOtklyuchenie();
                    log.info("[Чат] Отключение WebSocket userId={}",
                            accessor.getUser() != null ? accessor.getUser().getName() : "неизвестно");
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

                    if (dest.startsWith("/user/") && !dest.startsWith("/user/queue")) {
                        String name = principal.getName();
                        if (!dest.startsWith("/user/" + name + "/") && !dest.equals("/user/" + name)) {
                            throw new IllegalArgumentException("Запрещена подписка на чужую user-очередь");
                        }
                    }

                    if (dest.startsWith("/topic/")) {
                        if (!dest.startsWith("/topic/chat.")) {
                            throw new IllegalArgumentException("Подписка на этот топик запрещена");
                        }
                        Long chatId = parseChatId(dest);
                        Long userId;
                        try {
                            userId = Long.parseLong(principal.getName());
                        } catch (NumberFormatException ex) {
                            throw new IllegalArgumentException("Некорректный идентификатор пользователя");
                        }
                        if (chatId == null || chatParticipantRepository.findByChatIdAndUserId(chatId, userId).isEmpty()) {
                            throw new IllegalArgumentException("Нет доступа к топику чата " + dest);
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

    private static Long parseChatId(String dest) {
        try {
            String idPart = dest.substring("/topic/chat.".length());
            int slash = idPart.indexOf('/');
            if (slash >= 0) {
                idPart = idPart.substring(0, slash);
            }
            return Long.parseLong(idPart);
        } catch (Exception e) {
            return null;
        }
    }
}
