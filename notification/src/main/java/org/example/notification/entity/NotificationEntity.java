package org.example.notification.entity;

import com.example.common.kafka.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notifications_receiver_created", columnList = "receiver_id, created_at"),
        @Index(name = "idx_notifications_receiver_read", columnList = "receiver_id, read"),
        @Index(name = "idx_notifications_like_lookup", columnList = "receiver_id, sender_id, type, target_id")
})
public class NotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId; // Кому

    @Column(name = "sender_id", nullable = false)
    private Long senderId;   // От кого

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @Column(name = "target_id")
    private Long targetId;   // айди поста, комментария или заявки

    @Column(name = "message", length = 500)
    private String message;  // Текст уведомления

    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "read", nullable = false)
    private boolean read = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime createdAt;

    @Column(name = "sender_first_name")
    private String senderFirstName;

    @Column(name = "sender_last_name")
    private String senderLastName;

    /** Для чатов: PERSONAL или GROUP:название */
    @Column(name = "context_label", length = 255)
    private String contextLabel;
}