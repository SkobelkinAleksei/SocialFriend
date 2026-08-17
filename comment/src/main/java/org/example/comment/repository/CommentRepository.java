package org.example.comment.repository;

import org.example.comment.entity.CommentEntity;
import org.example.comment.entity.CommentStatus;
import org.example.comment.entity.CommentTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, Long> {

    @Query("""
        SELECT ce
        FROM CommentEntity ce
        WHERE ce.postId= :postId
        AND ce.commentStatus= :commentStatus
        ORDER BY ce.createdAt ASC
    """)
    List<CommentEntity> findAllByPostIdAndStatusPublished(CommentStatus commentStatus, Long postId);

    Page<CommentEntity> findAllByPostIdAndCommentStatus(
            Long postId,
            CommentStatus commentStatus,
            Pageable pageable
    );

    long countByPostIdAndCommentStatus(Long postId, CommentStatus commentStatus);

    Page<CommentEntity> findAllByTargetTypeAndTargetIdAndCommentStatus(
            CommentTargetType targetType,
            Long targetId,
            CommentStatus commentStatus,
            Pageable pageable
    );

    long countByTargetTypeAndTargetIdAndCommentStatus(
            CommentTargetType targetType,
            Long targetId,
            CommentStatus commentStatus
    );

    @Query("""
            SELECT c.postId, COUNT(c)
            FROM CommentEntity c
            WHERE c.postId IN :postIds AND c.commentStatus = :commentStatus
            GROUP BY c.postId
            """)
    List<Object[]> countPublishedByPostIds(
            @Param("postIds") List<Long> postIds,
            @Param("commentStatus") CommentStatus commentStatus
    );
}