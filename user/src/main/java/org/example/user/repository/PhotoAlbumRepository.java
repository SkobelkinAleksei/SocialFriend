package org.example.user.repository;

import org.example.user.entity.PhotoAlbumEntity;
import org.example.user.entity.PhotoAlbumKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PhotoAlbumRepository extends JpaRepository<PhotoAlbumEntity, Long> {
    List<PhotoAlbumEntity> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    Optional<PhotoAlbumEntity> findByOwnerIdAndKind(Long ownerId, PhotoAlbumKind kind);
    Optional<PhotoAlbumEntity> findByIdAndOwnerId(Long id, Long ownerId);
}
