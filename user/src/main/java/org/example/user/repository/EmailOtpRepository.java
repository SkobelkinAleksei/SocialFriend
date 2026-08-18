package org.example.user.repository;

import org.example.user.entity.EmailOtpEntity;
import org.example.user.entity.EmailOtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailOtpRepository extends JpaRepository<EmailOtpEntity, Long> {

    Optional<EmailOtpEntity> findFirstByEmailAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
            String email, EmailOtpPurpose purpose);

    @Modifying
    @Query("""
            update EmailOtpEntity o
            set o.usedAt = :now
            where o.email = :email and o.purpose = :purpose and o.usedAt is null
            """)
    int expireOpen(@Param("email") String email,
                   @Param("purpose") EmailOtpPurpose purpose,
                   @Param("now") LocalDateTime now);
}
