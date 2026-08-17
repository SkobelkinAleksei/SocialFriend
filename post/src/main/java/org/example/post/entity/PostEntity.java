package org.example.post.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "posts")
public class PostEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 3000)
    private String content;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "post_photos", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "photo_url", length = 255)
    @OrderColumn(name = "photo_index")
    private List<String> photos = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime updatedAt;

    @Column(name = "status_post", nullable = false)
    @Enumerated(EnumType.STRING)
    private StatusPost statusPost;

    @Column(name = "comments_allowed", nullable = false)
    private boolean commentsAllowed;

    @Column(name = "views_count", nullable = false)
    @Builder.Default
    private long viewsCount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "hidden_reason", length = 20)
    private PostHiddenReason hiddenReason;
}
