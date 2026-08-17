package org.example.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtGlobalFilter — кто проходит без JWT и откуда берётся identity")
public class JwtGlobalFilterTest {

    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private GatewayFilterChain chain;

    @Test
    @DisplayName("OPTIONS проходит без токена, клиентский X-User-Id снимается")
    void optionsStripsSpoofedId() {
        JwtGlobalFilter filter = new JwtGlobalFilter(jwtUtils);
        when(chain.filter(any())).thenReturn(Mono.empty());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.options("http://localhost/api/v1/social/users/me")
                        .header("X-User-Id", "999")
                        .build()
        );

        filter.filter(exchange, chain).block();

        org.mockito.ArgumentCaptor<org.springframework.web.server.ServerWebExchange> captor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.web.server.ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertNull(captor.getValue().getRequest().getHeaders().getFirst("X-User-Id"));
    }

    @Test
    @DisplayName("Открытый login не требует JWT")
    void openLogin() {
        JwtGlobalFilter filter = new JwtGlobalFilter(jwtUtils);
        when(chain.filter(any())).thenReturn(Mono.empty());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("http://localhost/api/v1/social/auth/login").build()
        );

        filter.filter(exchange, chain).block();
        verify(chain).filter(any());
        assertEquals(null, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("Подделка open-path внутри чужого URL не срабатывает (нужен startsWith)")
    void openPathNotContains() {
        JwtGlobalFilter filter = new JwtGlobalFilter(jwtUtils);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("http://localhost/api/v1/social/users/me/api/v1/social/auth/login")
                        .build()
        );

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("GET публичного media без JWT пропускается")
    void publicMedia() {
        JwtGlobalFilter filter = new JwtGlobalFilter(jwtUtils);
        when(chain.filter(any())).thenReturn(Mono.empty());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("http://localhost/api/v1/social/posts/media/a.jpg").build()
        );

        filter.filter(exchange, chain).block();
        verify(chain).filter(any());
    }

    @Test
    @DisplayName("POST на media без JWT — 401")
    void mediaPostForbidden() {
        JwtGlobalFilter filter = new JwtGlobalFilter(jwtUtils);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("http://localhost/api/v1/social/posts/media/a.jpg").build()
        );

        filter.filter(exchange, chain).block();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("Валидный Bearer: X-User-Id ставится из JWT, клиентский заголовок перезаписывается")
    void bearerSetsUserId() {
        JwtGlobalFilter filter = new JwtGlobalFilter(jwtUtils);
        when(jwtUtils.isTokenValid("abc.def")).thenReturn(true);
        when(jwtUtils.extractUserId("abc.def")).thenReturn(12L);
        when(jwtUtils.extractRole("abc.def")).thenReturn("USER");
        when(chain.filter(any())).thenReturn(Mono.empty());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("http://localhost/api/v1/social/users/me")
                        .header("Authorization", "Bearer abc.def")
                        .header("X-User-Id", "1")
                        .build()
        );

        filter.filter(exchange, chain).block();

        org.mockito.ArgumentCaptor<org.springframework.web.server.ServerWebExchange> captor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.web.server.ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertEquals("12", captor.getValue().getRequest().getHeaders().getFirst("X-User-Id"));
        assertEquals("USER", captor.getValue().getRequest().getHeaders().getFirst("X-Platform-Role"));
    }

    @Test
    @DisplayName("WebSocket: JWT из query token, userId и token из query вычищаются")
    void websocketQueryToken() {
        JwtGlobalFilter filter = new JwtGlobalFilter(jwtUtils);
        when(jwtUtils.isTokenValid("ws-token")).thenReturn(true);
        when(jwtUtils.extractUserId("ws-token")).thenReturn(3L);
        when(jwtUtils.extractRole("ws-token")).thenReturn("USER");
        when(chain.filter(any())).thenReturn(Mono.empty());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("http://localhost/ws/chat?token=ws-token&userId=99").build()
        );

        filter.filter(exchange, chain).block();

        org.mockito.ArgumentCaptor<org.springframework.web.server.ServerWebExchange> captor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.web.server.ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        var uri = captor.getValue().getRequest().getURI();
        assertNull(captor.getValue().getRequest().getQueryParams().getFirst("token"));
        assertNull(captor.getValue().getRequest().getQueryParams().getFirst("userId"));
        assertEquals("3", captor.getValue().getRequest().getHeaders().getFirst("X-User-Id"));
        assertEquals("/ws/chat", uri.getPath());
    }

    @Test
    @DisplayName("ADMIN не ходит в районные API")
    void adminBlockedFromSocial() {
        JwtGlobalFilter filter = new JwtGlobalFilter(jwtUtils);
        when(jwtUtils.isTokenValid("adm")).thenReturn(true);
        when(jwtUtils.extractUserId("adm")).thenReturn(1L);
        when(jwtUtils.extractRole("adm")).thenReturn("ADMIN");
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("http://localhost/api/v1/social/friends")
                        .header("Authorization", "Bearer adm")
                        .build()
        );

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("ADMIN проходит в /admin и в /users/me")
    void adminAllowedModeration() {
        JwtGlobalFilter filter = new JwtGlobalFilter(jwtUtils);
        when(jwtUtils.isTokenValid("adm")).thenReturn(true);
        when(jwtUtils.extractUserId("adm")).thenReturn(1L);
        when(jwtUtils.extractRole("adm")).thenReturn("ADMIN");
        when(chain.filter(any())).thenReturn(Mono.empty());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("http://localhost/api/v1/social/admin/reports")
                        .header("Authorization", "Bearer adm")
                        .build()
        );

        filter.filter(exchange, chain).block();
        verify(chain).filter(any());
    }

    @Test
    @DisplayName("Забаненный сосед с валидным JWT не проходит")
    void bannedUserRejected() {
        BannedAccountGuard guard = org.mockito.Mockito.mock(BannedAccountGuard.class);
        when(jwtUtils.isTokenValid("abc.def")).thenReturn(true);
        when(jwtUtils.extractUserId("abc.def")).thenReturn(12L);
        when(jwtUtils.extractRole("abc.def")).thenReturn("USER");
        when(guard.isBlocked(12L)).thenReturn(Mono.just(true));
        JwtGlobalFilter filter = new JwtGlobalFilter(jwtUtils, guard);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("http://localhost/api/v1/social/users/me")
                        .header("Authorization", "Bearer abc.def")
                        .build()
        );

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }
}
