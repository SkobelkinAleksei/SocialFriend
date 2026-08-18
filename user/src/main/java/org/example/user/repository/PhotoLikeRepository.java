package org.example.user.repository;

import org.example.user.entity.PhotoLikeEntity;
import org.example.user.entity.PhotoLikeKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PhotoLikeRepository extends JpaRepository<PhotoLikeEntity, Long> {
    Optional<PhotoLikeEntity> findByLikerIdAndKindAndTargetKey(Long likerId, PhotoLikeKind kind, String targetKey);
    long countByKindAndTargetKeyAndActiveTrue(PhotoLikeKind kind, String targetKey);
    boolean existsByLikerIdAndKindAndTargetKeyAndActiveTrue(Long likerId, PhotoLikeKind kind, String targetKey);
    List<PhotoLikeEntity> findByLikerIdAndKindAndTargetKeyInAndActiveTrue(Long likerId, PhotoLikeKind kind, Collection<String> targetKeys);
    void deleteByKindAndTargetKeyIn(PhotoLikeKind kind, Collection<String> targetKeys);
}
