package org.example.user.repository;

import org.example.user.entity.AvatarHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AvatarHistoryRepository extends JpaRepository<AvatarHistoryEntity, Long> {
    List<AvatarHistoryEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<AvatarHistoryEntity> findByIdAndUserId(Long id, Long userId);
    Optional<AvatarHistoryEntity> findFirstByUserIdAndUrl(Long userId, String url);
}
