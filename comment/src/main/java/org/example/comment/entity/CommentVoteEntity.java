package org.example.comment.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "comment_votes", uniqueConstraints = {
        @UniqueConstraint(name = "unique_comment_user_vote", columnNames = {"comment_id", "user_id"})
})
public class CommentVoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "vote", nullable = false, length = 16)
    private CommentVoteType vote;
}
