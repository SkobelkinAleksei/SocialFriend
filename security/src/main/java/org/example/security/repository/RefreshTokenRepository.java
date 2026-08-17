package org.example.security.repository;

import org.example.security.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshTokenEntity t SET t.revoked = true WHERE t.userId = :userId AND t.revoked = false")
    int revokeAllActiveForUser(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshTokenEntity t SET t.revoked = true WHERE t.familyId = :familyId AND t.revoked = false")
    int revokeAllInFamily(@Param("familyId") String familyId);

    @Modifying
    @Query(value = """
            DELETE FROM refresh_tokens t
            USING (
                SELECT id FROM refresh_tokens
                WHERE expires_at < :now OR revoked = true
                LIMIT :batch
            ) d
            WHERE t.id = d.id
            """, nativeQuery = true)
    int deleteExpiredOrRevokedBatch(@Param("now") java.time.Instant now, @Param("batch") int batch);
}
