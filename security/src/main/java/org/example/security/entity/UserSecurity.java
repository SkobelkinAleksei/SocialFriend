package org.example.security.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "users_security")
@Getter
@Setter
public class UserSecurity {
    @Id
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "failed_attempts", nullable = false, columnDefinition = "integer not null default 0")
    private int failedAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    /** null / true = можно войти; false = удалён или забанен на районе */
    @Getter(lombok.AccessLevel.NONE)
    @Column(name = "enabled", columnDefinition = "boolean default true")
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "account_status", length = 20)
    private String accountStatus;

    @Column(name = "platform_role", length = 20)
    private String platformRole;

    @Column(name = "email_verified", columnDefinition = "boolean default true")
    private Boolean emailVerified = Boolean.TRUE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime updatedAt;

    public boolean isEmailVerified() {
        return emailVerified == null || Boolean.TRUE.equals(emailVerified);
    }

    public boolean isLockedNow() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    public boolean isEnabled() {
        return enabled == null || Boolean.TRUE.equals(enabled);
    }

    public String effectivePlatformRole() {
        if (platformRole == null || platformRole.isBlank()) {
            return "USER";
        }
        return platformRole.trim().toUpperCase();
    }
}
