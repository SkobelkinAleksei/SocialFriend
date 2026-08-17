package org.example.restclient.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("InternalApiKeyFilter — защита /api/v1/internal/**")
public class InternalApiKeyFilterTest {

    @Test
    @DisplayName("Публичный путь фильтр пропускает без проверки ключа")
    void publicPathNotFiltered() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/social/users/me");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    @DisplayName("Верный ключ — цепочка фильтров продолжается")
    void validKey() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/internal/users/1");
        request.addHeader(InternalApiKeyFilter.HEADER, "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("Неверный ключ — 401, дальше не идём")
    void wrongKey() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/internal/users/1");
        request.addHeader(InternalApiKeyFilter.HEADER, "other");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Нет ключа — 401")
    void missingKey() throws Exception {
        InternalApiKeyFilter filter = new InternalApiKeyFilter("secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/internal/users/1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        verify(chain, never()).doFilter(request, response);
    }
}
