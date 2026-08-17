package org.example.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(
            @Value("${app.cors.allowed-origins}") String allowedOriginsRaw
    ) {
        List<String> origins = Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setMaxAge(3600L);

        boolean localDev = false;
        for (String origin : origins) {
            if (origin.contains("*")) {
                config.addAllowedOriginPattern(origin);
            } else {
                config.addAllowedOrigin(origin);
            }
            if (origin.contains("localhost") || origin.contains("127.0.0.1")) {
                localDev = true;
            }
        }
        // Телефон в той же Wi‑Fi: страница с http://192.168.x.x:5173, иначе gateway отвечает 403
        // и форма входа показывает «неверные данные».
        if (localDev) {
            config.addAllowedOriginPattern("http://localhost:[*]");
            config.addAllowedOriginPattern("http://127.0.0.1:[*]");
            config.addAllowedOriginPattern("http://192.168.*.*:[*]");
            config.addAllowedOriginPattern("http://10.*.*.*:[*]");
        }

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/ws/**", config);
        return new CorsWebFilter(source);
    }
}
