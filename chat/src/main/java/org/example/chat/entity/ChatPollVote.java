package org.example.chat.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "chat_poll_votes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"poll_id", "option_id", "user_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatPollVote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "poll_id", nullable = false)
    private Long pollId;

    @Column(name = "option_id", nullable = false)
    private Long optionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}
