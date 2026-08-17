package org.example.gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtils {

    private final SecretKey jwtSecret;

    public JwtUtils(@Value("${app.jwt.secret}") String secret) {
        this.jwtSecret = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public long extractUserId(String token) {
        return Long.parseLong(extractPayload(token).getSubject());
    }

    public String extractRole(String token) {
        String role = extractPayload(token).get("role", String.class);
        return "ADMIN".equalsIgnoreCase(role) ? "ADMIN" : "USER";
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
