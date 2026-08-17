package org.example.security.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.security.entity.SecurityUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    private final SecretKey jwtSecret;
    private final long accessExpirationMs;

    public JwtUtils(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-expiration-ms}") long accessExpirationMs
    ) {
        this.jwtSecret = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMs = accessExpirationMs;
    }

    public String generateAccessToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        SecurityUserDetails securityUserDetails = (SecurityUserDetails) userDetails;
        return generateAccessToken(securityUserDetails.getId(), securityUserDetails.getPlatformRole());
    }

    public String generateAccessToken(Long userId) {
        return generateAccessToken(userId, "USER");
    }

    public String generateAccessToken(Long userId, String role) {
        String safeRole = "ADMIN".equalsIgnoreCase(role) ? "ADMIN" : "USER";
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", safeRole)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpirationMs))
                .signWith(jwtSecret)
                .compact();
    }

    public Long extractUserId(String token) {
        String subject = Jwts.parser()
                .verifyWith(jwtSecret)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
        return Long.valueOf(subject);
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(jwtSecret)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
