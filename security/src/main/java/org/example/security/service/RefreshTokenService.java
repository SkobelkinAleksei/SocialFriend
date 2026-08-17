package org.example.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.security.dto.ConsumedRefreshToken;
import org.example.security.entity.RefreshTokenEntity;
import org.example.security.exception.InvalidRefreshTokenException;
import org.example.security.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Transactional
    public String issueRefreshToken(Long userId) {
        return issueRefreshToken(userId, UUID.randomUUID().toString());
    }

    @Transactional
    public String issueRefreshToken(Long userId, String familyId) {
        String family = (familyId == null || familyId.isBlank())
                ? UUID.randomUUID().toString()
                : familyId;
        String rawToken = UUID.randomUUID() + "." + UUID.randomUUID();
        Instant now = Instant.now();

        RefreshTokenEntity entity = RefreshTokenEntity.builder()
                .userId(userId)
                .familyId(family)
                .tokenHash(hash(rawToken))
                .expiresAt(now.plusMillis(refreshExpirationMs))
                .revoked(false)
                .createdAt(now)
                .build();

        refreshTokenRepository.save(entity);
        return rawToken;
    }

    @Transactional
    public ConsumedRefreshToken validateAndConsume(String rawRefreshToken) {
        RefreshTokenEntity entity = refreshTokenRepository
                .findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (entity.isRevoked()) {
            log.warn("[RefreshToken] Reuse family={} user={}", entity.getFamilyId(), entity.getUserId());
            revokeFamilyOrUser(entity);
            throw new InvalidRefreshTokenException();
        }

        if (entity.getExpiresAt().isBefore(Instant.now())) {
            entity.setRevoked(true);
            refreshTokenRepository.save(entity);
            throw new InvalidRefreshTokenException();
        }

        entity.setRevoked(true);
        refreshTokenRepository.save(entity);
        return new ConsumedRefreshToken(entity.getUserId(), entity.getFamilyId());
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken))
                .ifPresent(token -> {
                    if (!token.isRevoked()) {
                        token.setRevoked(true);
                        refreshTokenRepository.save(token);
                    }
                });
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllActiveForUser(userId);
    }

    @Transactional
    public int deleteExpiredOrRevoked(int batchSize) {
        int total = 0;
        int batch = Math.max(1, Math.min(batchSize, 1000));
        for (int i = 0; i < 50; i++) {
            int deleted = refreshTokenRepository.deleteExpiredOrRevokedBatch(Instant.now(), batch);
            total += deleted;
            if (deleted < batch) {
                break;
            }
        }
        log.info("[RefreshToken] Удалено просроченных/отозванных токенов: {}", total);
        return total;
    }

    private void revokeFamilyOrUser(RefreshTokenEntity entity) {
        if (entity.getFamilyId() == null || entity.getFamilyId().isBlank()) {
            refreshTokenRepository.revokeAllActiveForUser(entity.getUserId());
            return;
        }
        refreshTokenRepository.revokeAllInFamily(entity.getFamilyId());
    }

    private static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 недоступен", e);
        }
    }
}
