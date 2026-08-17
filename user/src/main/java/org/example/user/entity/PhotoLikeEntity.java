package org.example.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "photo_likes", uniqueConstraints = {
        @UniqueConstraint(name = "unique_photo_like", columnNames = {"liker_id", "kind", "target_key"})
})
public class PhotoLikeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "liker_id", nullable = false)
    private Long likerId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private PhotoLikeKind kind;

    @Column(name = "target_key", nullable = false, length = 220)
    private String targetKey;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "context_label", length = 255)
    private String contextLabel;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime updatedAt;
}
