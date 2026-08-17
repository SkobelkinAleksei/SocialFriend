package org.example.restclient.config;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class InternalApiKeyInterceptor implements ClientHttpRequestInterceptor {

    private final String apiKey;

    public InternalApiKeyInterceptor(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        if (!apiKey.isBlank()
                && isInternalPath(request)
                && !request.getHeaders().containsKey(InternalApiKeyFilter.HEADER)) {
            request.getHeaders().set(InternalApiKeyFilter.HEADER, apiKey);
        }
        return execution.execute(request, body);
    }

    private static boolean isInternalPath(HttpRequest request) {
        String path = request.getURI().getPath();
        return path != null && path.contains("/api/v1/internal/");
    }
}
