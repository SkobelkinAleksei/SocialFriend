package org.example.restclient.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

public class SecurityHeaderInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest currentRequest = attributes.getRequest();
            // Вытаскиваем токен из входящего запроса
            String authHeader = currentRequest.getHeader("Authorization");

            if (authHeader != null) {
                // Вставляем его в исходящий запрос RestTemplate
                request.getHeaders().add("Authorization", authHeader);
            }
        }

        return execution.execute(request, body);
    }
}