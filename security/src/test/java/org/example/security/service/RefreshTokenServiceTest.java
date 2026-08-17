package org.example.security.service;

import org.example.security.dto.ConsumedRefreshToken;
import org.example.security.entity.RefreshTokenEntity;
import org.example.security.exception.InvalidRefreshTokenException;
import org.example.security.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService — выпуск, ротация и защита от reuse")
public class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService service;

    @BeforeEach
    void ttl() {
        ReflectionTestUtils.setField(service, "refreshExpirationMs", 86_400_000L);
    }

    @Test
    @DisplayName("issueRefreshToken сохраняет только хеш, наружу отдаёт сырой токен")
    void issueStoresHash() {
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String raw = service.issueRefreshToken(5L, "family-1");

        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(RefreshTokenEntity.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshTokenEntity stored = captor.getValue();
        assertEquals(5L, stored.getUserId());
        assertEquals("family-1", stored.getFamilyId());
        assertEquals(sha256(raw), stored.getTokenHash());
        assertTrue(raw.contains("."));
    }

    @Nested
    @DisplayName("validateAndConsume")
    class Consume {

        @Test
        @DisplayName("Неизвестный токен — InvalidRefreshTokenException")
        void unknown() {
            when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());
            assertThrows(InvalidRefreshTokenException.class, () -> service.validateAndConsume("raw.token"));
        }

        @Test
        @DisplayName("Повторное использование отозванного токена отзывает всю семью")
        void reuseRevokesFamily() {
            RefreshTokenEntity entity = token(false);
            entity.setRevoked(true);
            entity.setFamilyId("fam");
            when(refreshTokenRepository.findByTokenHash(sha256("raw.token"))).thenReturn(Optional.of(entity));

            assertThrows(InvalidRefreshTokenException.class, () -> service.validateAndConsume("raw.token"));
            verify(refreshTokenRepository).revokeAllInFamily("fam");
        }

        @Test
        @DisplayName("Просроченный токен помечается revoked и отвергается")
        void expired() {
            RefreshTokenEntity entity = token(false);
            entity.setExpiresAt(Instant.now().minusSeconds(10));
            when(refreshTokenRepository.findByTokenHash(sha256("raw.token"))).thenReturn(Optional.of(entity));

            assertThrows(InvalidRefreshTokenException.class, () -> service.validateAndConsume("raw.token"));
            assertTrue(entity.isRevoked());
            verify(refreshTokenRepository).save(entity);
        }

        @Test
        @DisplayName("Живой токен потребляется: revoked=true, возвращаются userId и family")
        void success() {
            RefreshTokenEntity entity = token(false);
            when(refreshTokenRepository.findByTokenHash(sha256("raw.token"))).thenReturn(Optional.of(entity));

            ConsumedRefreshToken consumed = service.validateAndConsume("raw.token");

            assertEquals(8L, consumed.userId());
            assertEquals("fam-8", consumed.familyId());
            assertTrue(entity.isRevoked());
        }
    }

    @Test
    @DisplayName("revoke уже отозванного токена повторно не сохраняет")
    void revokeIdempotent() {
        RefreshTokenEntity entity = token(true);
        when(refreshTokenRepository.findByTokenHash(sha256("raw.token"))).thenReturn(Optional.of(entity));

        service.revoke("raw.token");

        verify(refreshTokenRepository, never()).save(any());
    }

    private static RefreshTokenEntity token(boolean revoked) {
        return RefreshTokenEntity.builder()
                .id(1L)
                .userId(8L)
                .familyId("fam-8")
                .tokenHash(sha256("raw.token"))
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(revoked)
                .createdAt(Instant.now())
                .build();
    }

    private static String sha256(String raw) {
        try {
            byte[] hashed = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
