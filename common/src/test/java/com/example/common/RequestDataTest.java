package com.example.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RequestData — контракт S2S-запроса")
public class RequestDataTest {

    @Test
    @DisplayName("Короткий конструктор даёт пустые заголовки, без поля data")
    void shortConstructor() {
        RequestData request = new RequestData("http://friend/api");
        assertEquals("http://friend/api", request.url());
        assertTrue(request.headers().isEmpty());
    }

    @Test
    @DisplayName("Полный конструктор сохраняет заголовки как есть")
    void withHeaders() {
        RequestData request = new RequestData("http://x", Map.of("X-Internal-Key", "k"));
        assertEquals("k", request.headers().get("X-Internal-Key"));
    }
}
