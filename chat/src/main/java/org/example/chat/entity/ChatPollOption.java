package org.example.chat.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_poll_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatPollOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    private ChatPoll poll;

    @Column(nullable = false, length = 100)
    private String text;

    @Column(nullable = false)
    private int position;
}
