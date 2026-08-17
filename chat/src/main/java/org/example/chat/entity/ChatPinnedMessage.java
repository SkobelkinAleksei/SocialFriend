package org.example.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_pinned_messages", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"message_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatPinnedMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "dm_user_min")
    private Long dmUserMin;

    @Column(name = "dm_user_max")
    private Long dmUserMax;

    @Column(name = "pinned_by", nullable = false)
    private Long pinnedBy;

    @Column(name = "pinned_at", nullable = false)
    private LocalDateTime pinnedAt;
}
