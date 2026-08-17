package org.example.user.repository;

import org.example.user.entity.GalleryPhotoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GalleryPhotoRepository extends JpaRepository<GalleryPhotoEntity, Long> {
    Page<GalleryPhotoEntity> findByOwnerIdOrderByCreatedAtDesc(Long ownerId, Pageable pageable);
    Page<GalleryPhotoEntity> findByOwnerIdAndAlbumIdOrderByCreatedAtDesc(Long ownerId, Long albumId, Pageable pageable);

    @Query("""
            SELECT p FROM GalleryPhotoEntity p
            WHERE p.ownerId = :ownerId
              AND p.albumId NOT IN (
                SELECT a.id FROM PhotoAlbumEntity a
                WHERE a.ownerId = :ownerId AND a.kind = org.example.user.entity.PhotoAlbumKind.SAVED
              )
            ORDER BY p.createdAt DESC
            """)
    Page<GalleryPhotoEntity> findPublicTimeline(@Param("ownerId") Long ownerId, Pageable pageable);
    List<GalleryPhotoEntity> findByAlbumIdOrderByCreatedAtDesc(Long albumId);
    long countByAlbumId(Long albumId);
    Optional<GalleryPhotoEntity> findFirstByAlbumIdOrderByCreatedAtDesc(Long albumId);
    Optional<GalleryPhotoEntity> findByIdAndOwnerId(Long id, Long ownerId);
    Optional<GalleryPhotoEntity> findByOwnerIdAndUrl(Long ownerId, String url);
}
