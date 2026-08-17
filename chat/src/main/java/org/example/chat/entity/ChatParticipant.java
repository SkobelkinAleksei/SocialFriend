package org.example.chat.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_participants", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"chat_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id")
    private ChatRoom chat;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "unread_count", nullable = false)
    @Builder.Default
    private int unreadCount = 0;

    @Column(nullable = false, columnDefinition = "boolean not null default false")
    @Builder.Default
    private boolean admin = false;
}
