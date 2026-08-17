package org.example.restclient.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("InternalApiKeyInterceptor — ключ только на internal-путях")
public class InternalApiKeyInterceptorTest {

    @Test
    @DisplayName("Internal URL без ключа — подставляем X-Internal-Key")
    void addsKeyOnInternal() throws Exception {
        InternalApiKeyInterceptor interceptor = new InternalApiKeyInterceptor("secret");
        MockClientHttpRequest request = new MockClientHttpRequest();
        request.setURI(URI.create("http://user:8081/api/v1/internal/users/1"));
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(execution.execute(any(HttpRequest.class), any())).thenReturn(response);

        interceptor.intercept(request, new byte[0], execution);

        assertEquals("secret", request.getHeaders().getFirst(InternalApiKeyFilter.HEADER));
        verify(execution).execute(eq(request), any());
    }

    @Test
    @DisplayName("Публичный URL — ключ не добавляем")
    void skipsPublicPath() throws Exception {
        InternalApiKeyInterceptor interceptor = new InternalApiKeyInterceptor("secret");
        MockClientHttpRequest request = new MockClientHttpRequest();
        request.setURI(URI.create("http://user:8081/api/v1/social/users/1"));
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mock(ClientHttpResponse.class));

        interceptor.intercept(request, new byte[0], execution);

        assertNull(request.getHeaders().getFirst(InternalApiKeyFilter.HEADER));
    }

    @Test
    @DisplayName("Ключ уже стоит — не перезаписываем")
    void keepsExistingHeader() throws Exception {
        InternalApiKeyInterceptor interceptor = new InternalApiKeyInterceptor("secret");
        MockClientHttpRequest request = new MockClientHttpRequest();
        request.setURI(URI.create("http://x/api/v1/internal/ping"));
        request.getHeaders().set(InternalApiKeyFilter.HEADER, "already");
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mock(ClientHttpResponse.class));

        interceptor.intercept(request, new byte[0], execution);

        assertEquals("already", request.getHeaders().getFirst(InternalApiKeyFilter.HEADER));
    }

    @Test
    @DisplayName("Пустой ключ в конфиге — заголовок не ставим")
    void blankKey() throws Exception {
        InternalApiKeyInterceptor interceptor = new InternalApiKeyInterceptor("  ");
        MockClientHttpRequest request = new MockClientHttpRequest();
        request.setURI(URI.create("http://x/api/v1/internal/ping"));
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        when(execution.execute(any(), any())).thenReturn(mock(ClientHttpResponse.class));

        interceptor.intercept(request, new byte[0], execution);

        assertNull(request.getHeaders().getFirst(InternalApiKeyFilter.HEADER));
    }
}
