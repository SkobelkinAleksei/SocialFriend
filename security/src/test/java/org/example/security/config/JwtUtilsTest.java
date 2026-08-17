package org.example.security.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.security.entity.SecurityUserDetails;
import org.example.security.entity.UserSecurity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("JwtUtils (security) — выпуск и проверка access-токена")
public class JwtUtilsTest {

    private static final String SECRET = "test-secret-must-be-at-least-32-bytes-long-for-hs256!!";
    private final JwtUtils jwtUtils = new JwtUtils(SECRET, 60_000);

    @Test
    @DisplayName("Токен выпускается с subject = userId и проходит проверку")
    void generateAndValidate() {
        String token = jwtUtils.generateAccessToken(42L);

        assertTrue(jwtUtils.isTokenValid(token));
        assertEquals(42L, jwtUtils.extractUserId(token));
        assertEquals("USER", roleOf(token));
    }

    @Test
    @DisplayName("Токен админа содержит claim role=ADMIN")
    void adminRoleClaim() {
        String token = jwtUtils.generateAccessToken(8L, "ADMIN");
        assertEquals("ADMIN", roleOf(token));
    }

    @Test
    @DisplayName("Токен из Authentication берёт id из SecurityUserDetails")
    void fromAuthentication() {
        UserSecurity user = new UserSecurity();
        user.setId(7L);
        user.setUsername("a@b.c");
        user.setPassword("x");
        var auth = new UsernamePasswordAuthenticationToken(new SecurityUserDetails(user), null);

        String token = jwtUtils.generateAccessToken(auth);

        assertEquals(7L, jwtUtils.extractUserId(token));
    }

    @Test
    @DisplayName("Подделанный или пустой токен невалиден")
    void invalidToken() {
        assertFalse(jwtUtils.isTokenValid("not-a-jwt"));
        assertFalse(jwtUtils.isTokenValid(null));
        String other = new JwtUtils(SECRET.replace('t', 'z'), 60_000).generateAccessToken(1L);
        assertFalse(jwtUtils.isTokenValid(other));
    }

    private String roleOf(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }
}
