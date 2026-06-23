package org.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:5173");
        config.addAllowedOrigin("http://localhost:5174");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // 1. Применяем глобальный CORS ко всем стандартным REST-запросам API
        source.registerCorsConfiguration("/api/**", config);

        // 2. Применяем глобальный CORS к чату (если там в WebSocketConfig вы НЕ прописывали домены)
        source.registerCorsConfiguration("/ws-chat/**", config);
        return new CorsWebFilter(source);
    }

}
