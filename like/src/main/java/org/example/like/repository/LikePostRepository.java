package org.example.like.repository;

import jakarta.persistence.LockModeType;
import org.example.like.entity.LikePostEntity;
import org.example.like.entity.LikeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikePostRepository extends JpaRepository<LikePostEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM LikePostEntity l WHERE l.postId = :postId AND l.userId = :userId")
    Optional<LikePostEntity> findByPostIdAndUserIdForUpdate(
            @Param("postId") Long postId,
            @Param("userId") Long userId
    );

    Optional<LikePostEntity> findByPostIdAndUserId(Long postId, Long userId);

    @Query("""
    SELECT lp
    FROM LikePostEntity lp
    WHERE lp.postId = :postId
        AND lp.likeStatus = :status
""")
    List<LikePostEntity> findAllActiveByPostId(
            @Param("postId") Long postId,
            @Param("status") LikeStatus status
    );

    default List<LikePostEntity> findAllActiveByPostId(Long postId) {
        return findAllActiveByPostId(postId, LikeStatus.ACTIVE);
    }

    @Query("SELECT COUNT(l) FROM LikePostEntity l WHERE l.postId = :postId AND l.likeStatus = :status")
    long countActiveLikesByPostId(@Param("postId") Long postId, @Param("status") LikeStatus status);

    default long countActiveLikesByPostId(Long postId) {
        return countActiveLikesByPostId(postId, LikeStatus.ACTIVE);
    }

    boolean existsByPostIdAndUserIdAndLikeStatus(Long postId, Long userId, LikeStatus likeStatus);

    Long countByPostIdAndLikeStatus(Long postId, LikeStatus likeStatus);

    @Query("""
            SELECT l.postId, COUNT(l)
            FROM LikePostEntity l
            WHERE l.postId IN :postIds AND l.likeStatus = :status
            GROUP BY l.postId
            """)
    List<Object[]> countActiveLikesGroupedByPostId(
            @Param("postIds") List<Long> postIds,
            @Param("status") LikeStatus status
    );

    @Query("""
            SELECT l.postId
            FROM LikePostEntity l
            WHERE l.userId = :userId
              AND l.likeStatus = :status
              AND l.postId IN :postIds
            """)
    List<Long> findActiveLikedPostIds(
            @Param("userId") Long userId,
            @Param("postIds") List<Long> postIds,
            @Param("status") LikeStatus status
    );
}
