package org.example.scheduler.client;

import lombok.RequiredArgsConstructor;
import org.example.restclient.config.IHttpCore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityInternalClient {

    private final IHttpCore httpCore;

    @Value("${app.services.security-base-url}")
    private String securityBaseUrl;

    public void cleanupExpiredRefreshTokens() {
        String url = securityBaseUrl + "/api/v1/internal/security/refresh-tokens/expired";
        httpCore.post(url, HttpMethod.POST, HttpEntity.EMPTY, Void.class);
    }
}
