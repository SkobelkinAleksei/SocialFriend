package org.example.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration
public class GatewayConfig {

    private final String dadataApiKey;
    private final String userUrl;
    private final String friendUrl;
    private final String postUrl;
    private final String commentUrl;
    private final String likeUrl;
    private final String securityUrl;
    private final String chatUrl;
    private final String chatWsUrl;
    private final String notificationUrl;
    private final String notificationWsUrl;
    private final String eventUrl;

    public GatewayConfig(
            @Value("${app.dadata.api-key}") String dadataApiKey,
            @Value("${app.services.user}") String userUrl,
            @Value("${app.services.friend}") String friendUrl,
            @Value("${app.services.post}") String postUrl,
            @Value("${app.services.comment}") String commentUrl,
            @Value("${app.services.like}") String likeUrl,
            @Value("${app.services.security}") String securityUrl,
            @Value("${app.services.chat}") String chatUrl,
            @Value("${app.services.chat-ws}") String chatWsUrl,
            @Value("${app.services.notification}") String notificationUrl,
            @Value("${app.services.notification-ws}") String notificationWsUrl,
            @Value("${app.services.event}") String eventUrl
    ) {
        this.dadataApiKey = dadataApiKey;
        this.userUrl = userUrl;
        this.friendUrl = friendUrl;
        this.postUrl = postUrl;
        this.commentUrl = commentUrl;
        this.likeUrl = likeUrl;
        this.securityUrl = securityUrl;
        this.chatUrl = chatUrl;
        this.chatWsUrl = chatWsUrl;
        this.notificationUrl = notificationUrl;
        this.notificationWsUrl = notificationWsUrl;
        this.eventUrl = eventUrl;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user", r -> r
                        .path("/api/v1/social/users/**")
                        .uri(userUrl))

                .route("reports", r -> r
                        .path("/api/v1/social/reports/**")
                        .uri(userUrl))

                .route("admin_user", r -> r
                        .path(
                                "/api/v1/social/admin/reports/**",
                                "/api/v1/social/admin/users/**",
                                "/api/v1/social/admin/staff/**",
                                "/api/v1/social/admin/photos/**"
                        )
                        .uri(userUrl))

                .route("admin_comment", r -> r
                        .path("/api/v1/social/admin/comments/**")
                        .uri(commentUrl))

                .route("admin_post", r -> r
                        .path("/api/v1/social/admin/posts/**")
                        .uri(postUrl))

                .route("admin_event", r -> r
                        .path("/api/v1/social/admin/events/**")
                        .uri(eventUrl))

                .route("admin_chat", r -> r
                        .path("/api/v1/social/admin/chats/**")
                        .uri(chatUrl))

                .route("register", r -> r
                        .path("/api/v1/social/registration/**")
                        .uri(userUrl))

                .route("friend", r -> r
                        .path("/api/v1/social/friends/**")
                        .uri(friendUrl))

                .route("post", r -> r
                        .path("/api/v1/social/posts/**")
                        .uri(postUrl))

                .route("comment", r -> r
                        .path("/api/v1/social/comments/**")
                        .uri(commentUrl))

                .route("like", r -> r
                        .path("/api/v1/social/likes/**")
                        .uri(likeUrl))

                .route("security", r -> r
                        .path("/api/v1/social/auth/**")
                        .uri(securityUrl))

                .route("chat_http", r -> r
                        .path("/api/v1/social/chats/**")
                        .uri(chatUrl))

                .route("chat_ws", r -> r
                        .path("/ws/chat/**")
                        .uri(chatWsUrl))

                .route("notification_http", r -> r
                        .path("/api/v1/social/notifications/**")
                        .uri(notificationUrl))

                .route("notification_ws", r -> r
                        .path("/ws/notifications/**")
                        .uri(notificationWsUrl))

                .route("event", r -> r
                        .path("/api/v1/social/events/**")
                        .uri(eventUrl))

                .route("dadata_suggestions", r -> r
                        .path("/api/v1/geo/suggest/**")
                        .filters(f -> f
                                .rewritePath("/api/v1/geo/suggest/(?<remaining>.*)", "/suggestions/api/4_1/rs/suggest/${remaining}")
                                .removeRequestHeader(HttpHeaders.AUTHORIZATION)
                                .addRequestHeader(HttpHeaders.AUTHORIZATION, "Token " + dadataApiKey)
                                .dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_FIRST")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_FIRST")
                        )
                        .uri("https://suggestions.dadata.ru"))

                .build();
    }
}
