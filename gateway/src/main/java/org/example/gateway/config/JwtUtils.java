package org.example.gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtils {

    private final SecretKey jwtSecret = Keys.hmacShaKeyFor(
            "your-very-strong-secret-key-32-characters-long".getBytes(StandardCharsets.UTF_8)
    );

    public long extractUserId(String token) {
        return Long.parseLong(extractPayload(token).getSubject());
    }

    private Claims extractPayload(String token) {
        return Jwts.parser()
                .verifyWith(jwtSecret)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            extractPayload(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}