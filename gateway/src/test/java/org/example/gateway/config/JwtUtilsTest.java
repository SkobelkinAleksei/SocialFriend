package org.example.gateway.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("JwtUtils (gateway) — разбор access-токена")
public class JwtUtilsTest {

    private static final String SECRET = "gateway-test-secret-must-be-32-bytes-minimum!!!!";
    private final JwtUtils jwtUtils = new JwtUtils(SECRET);

    @Test
    @DisplayName("Валидный токен: isTokenValid=true и subject = userId")
    void validToken() {
        String token = signed(SECRET, "15", System.currentTimeMillis() + 60_000);

        assertTrue(jwtUtils.isTokenValid(token));
        assertEquals(15L, jwtUtils.extractUserId(token));
        assertEquals("USER", jwtUtils.extractRole(token));
    }

    @Test
    @DisplayName("Claim role=ADMIN читается как ADMIN")
    void adminRole() {
        String token = signed(SECRET, "2", System.currentTimeMillis() + 60_000, "ADMIN");
        assertEquals("ADMIN", jwtUtils.extractRole(token));
    }

    @Test
    @DisplayName("Просроченный токен невалиден")
    void expired() {
        String token = signed(SECRET, "1", System.currentTimeMillis() - 5_000);
        assertFalse(jwtUtils.isTokenValid(token));
    }

    @Test
    @DisplayName("Чужая подпись невалидна")
    void wrongKey() {
        String token = signed("other-secret-must-be-32-bytes-minimum-xxxxx!!", "1", System.currentTimeMillis() + 60_000);
        assertFalse(jwtUtils.isTokenValid(token));
    }

    private static String signed(String secret, String subject, long expMs) {
        return signed(secret, subject, expMs, null);
    }

    private static String signed(String secret, String subject, long expMs, String role) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        var builder = Jwts.builder()
                .subject(subject)
                .expiration(new Date(expMs));
        if (role != null) {
            builder.claim("role", role);
        }
        return builder.signWith(key).compact();
    }
}
