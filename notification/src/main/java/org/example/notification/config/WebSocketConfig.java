package org.example.notification.config;

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

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Регистрируем ту же точку входа для вебсокетов, что и в чате
        registry.addEndpoint("/ws/notifications") // 👈 Сделайте путь уникальным!
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Настраиваем префиксы очередей
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setUserDestinationPrefix("/user");
    }

    // 💡 ДОБАВИТЬ: Перехватчик для авторизации WebSocket-сессий
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // 💡 Читаем заголовок, который мы только что отправили из App.vue
                    String userId = accessor.getFirstNativeHeader("X-User-Id");

                    if (userId != null) {
                        final String finalUserId = userId;
                        accessor.setUser(new Principal() {
                            @Override
                            public String getName() {
                                return finalUserId;
                            }
                        });
                        // Лог в консоль микросервиса для проверки, что юзер распознан:
                        System.out.println("[WebSocket-Сессия] Пользователь " + finalUserId + " успешно авторизован в WebSocket!");
                    } else {
                        System.out.println("[WebSocket-Сессия] ПРЕДУПРЕЖДЕНИЕ: Заголовок X-User-Id не найден в CONNECT!");
                    }
                }
                return message;
            }
        });
    }

}
