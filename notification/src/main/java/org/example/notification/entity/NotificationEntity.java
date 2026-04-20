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
@Table(name = "notifications")
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

    @Column(name = "message")
    private String message;  // Текст уведомления

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime createdAt;
}