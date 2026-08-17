package org.example.post.repository;

import org.example.post.entity.PostViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostViewRepository extends JpaRepository<PostViewEntity, Long> {
    boolean existsByPostIdAndUserId(Long postId, Long userId);
}
