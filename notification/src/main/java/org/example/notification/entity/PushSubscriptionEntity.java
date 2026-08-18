package org.example.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "push_subscriptions",
        uniqueConstraints = @UniqueConstraint(name = "uk_push_endpoint", columnNames = "endpoint")
)
public class PushSubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 1024)
    private String endpoint;

    @Column(nullable = false, length = 255)
    private String p256dh;

    @Column(nullable = false, length = 255)
    private String auth;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** До этого момента устройство смотрит сайт — системный push туда не шлём. */
    @Column(name = "foreground_until")
    private Instant foregroundUntil;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
