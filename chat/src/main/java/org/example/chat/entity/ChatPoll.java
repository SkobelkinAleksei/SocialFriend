package org.example.chat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_polls")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatPoll {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, unique = true)
    private Long messageId;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(nullable = false, length = 255)
    private String question;

    @Column(nullable = false)
    @Builder.Default
    private boolean anonymous = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean multiple = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean closed = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ChatPollPurpose purpose = ChatPollPurpose.USER;

    @Column(name = "closes_at")
    private LocalDateTime closesAt;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "chat_poll_eligible_voters", joinColumns = @JoinColumn(name = "poll_id"))
    @Column(name = "user_id")
    private java.util.Set<Long> eligibleVoterIds = new java.util.HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @Builder.Default
    private List<ChatPollOption> options = new ArrayList<>();
}
