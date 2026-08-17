package org.example.friend.repository;

import org.example.friend.entity.FriendBlockListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendBlockListRepository extends JpaRepository<FriendBlockListEntity, Long> {

    boolean existsByAuthorIdAndBlockedUserId(Long authorId, Long blockedUserId);

    Optional<FriendBlockListEntity> findByAuthorIdAndBlockedUserId(Long authorId, Long blockedUserId);

    List<FriendBlockListEntity> findByAuthorId(Long authorId);

    List<FriendBlockListEntity> findByBlockedUserId(Long blockedUserId);

    void deleteByAuthorIdAndBlockedUserId(Long authorId, Long blockedUserId);
}
