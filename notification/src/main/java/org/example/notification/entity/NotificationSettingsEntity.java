package org.example.notification.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_notification_settings")
public class NotificationSettingsEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Builder.Default
    @Column(name = "notify_comments", nullable = false)
    private Boolean notifyComments = true;

    @Builder.Default
    @Column(name = "notify_messages", nullable = false)
    private Boolean notifyMessages = true;

    @Builder.Default
    @Column(name = "notify_event_requests", nullable = false)
    private Boolean notifyEventRequests = true;

    @Builder.Default
    @Column(name = "notify_reputation", nullable = false)
    private Boolean notifyReputation = true;
}
