package org.example.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;

@Configuration
@EnableWebFlux
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user", r -> r
                        .path("/api/v1/social/users/**")
                        .uri("http://localhost:8081"))

                .route("register", r -> r
                        .path("/api/v1/social/registration/**")
                        .uri("http://localhost:8081"))

                .route("friend", r -> r
                        .path("/api/v1/social/friends/**")
                        .uri("http://localhost:8082"))

                .route("post", r -> r
                        .path("/api/v1/social/posts/**")
                        .uri("http://localhost:8083"))

                .route("comment", r -> r
                        .path("/api/v1/social/comments/**")
                        .uri("http://localhost:8084"))

                .route("security", r -> r
                        .path("/api/v1/social/auth/**")
                        .uri("http://localhost:8888"))
                .build();
    }
}