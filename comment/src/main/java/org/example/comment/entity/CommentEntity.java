package org.example.comment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@Entity
@Table(name = "comments", indexes = {
        @Index(name = "idx_comments_post_status_created", columnList = "post_id, comment_status, created_at"),
        @Index(name = "idx_comments_target_status_created", columnList = "target_type, target_id, comment_status, created_at")
})
public class CommentEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(name = "post_id")
    private Long postId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 20)
    private CommentTargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "reply_to_user_id")
    private Long replyToUserId;

    @Column(name = "reply_to_comment_id")
    private Long replyToCommentId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "comment_status")
    private CommentStatus commentStatus;
}
