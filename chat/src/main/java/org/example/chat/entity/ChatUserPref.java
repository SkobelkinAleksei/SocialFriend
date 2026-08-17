package org.example.chat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_user_prefs", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "scope_key"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatUserPref {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** P:{partnerId} for DM, R:{chatId} for any room */
    @Column(name = "scope_key", nullable = false, length = 40)
    private String scopeKey;

    @Column(nullable = false)
    @Builder.Default
    private boolean muted = false;

    @Column(name = "pinned_at")
    private LocalDateTime pinnedAt;
}
