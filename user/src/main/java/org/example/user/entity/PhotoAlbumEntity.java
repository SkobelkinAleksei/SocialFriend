package org.example.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "photo_albums")
public class PhotoAlbumEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "title", nullable = false, length = 80)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private PhotoAlbumKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "cover_mode", nullable = false, length = 20)
    @Builder.Default
    private AlbumCoverMode coverMode = AlbumCoverMode.LATEST;

    @Column(name = "cover_photo_id")
    private Long coverPhotoId;

    /** Null у старых строк допустим: читаем как 0, пока колонка не заполнится. */
    @Builder.Default
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, columnDefinition = "TIMESTAMP(0)")
    private LocalDateTime createdAt;

    @PrePersist
    void fillSortOrder() {
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }

    public int sortValue() {
        return sortOrder == null ? 0 : sortOrder;
    }
}
