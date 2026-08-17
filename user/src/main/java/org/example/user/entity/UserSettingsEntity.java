package org.example.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettingsEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserEntity user;

    // Колонка оставлена для совместимости схемы; радиус поиска больше не используется
    @Column(name = "search_radius", nullable = false)
    @Builder.Default
    private Double searchRadius = 1.0;

    // --- ПРИВАТНОСТЬ ---
    @Column(name = "allow_dm_from_all", nullable = false)
    @Builder.Default
    private Boolean allowDmFromAll = true;

    @Column(name = "allow_comments_from_all", nullable = false)
    @Builder.Default
    private Boolean allowCommentsFromAll = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "photo_visibility", length = 20)
    @Builder.Default
    private PhotoVisibility photoVisibility = PhotoVisibility.ALL;

    // --- УВЕДОМЛЕНИЯ ---
    @Column(name = "notify_comments", nullable = false)
    @Builder.Default
    private Boolean notifyComments = true;

    @Column(name = "notify_messages", nullable = false)
    @Builder.Default
    private Boolean notifyMessages = true;

    @Column(name = "notify_event_requests", nullable = false)
    @Builder.Default
    private Boolean notifyEventRequests = true;

    @Column(name = "notify_reputation", nullable = false)
    @Builder.Default
    private Boolean notifyReputation = true;

    @Column(name = "show_last_seen")
    @Builder.Default
    private Boolean showLastSeen = true;
}