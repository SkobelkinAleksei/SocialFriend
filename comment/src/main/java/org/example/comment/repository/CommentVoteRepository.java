package org.example.comment.repository;

import jakarta.persistence.LockModeType;
import org.example.comment.entity.CommentVoteEntity;
import org.example.comment.entity.CommentVoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommentVoteRepository extends JpaRepository<CommentVoteEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM CommentVoteEntity v WHERE v.commentId = :commentId AND v.userId = :userId")
    Optional<CommentVoteEntity> findByCommentIdAndUserIdForUpdate(
            @Param("commentId") Long commentId,
            @Param("userId") Long userId
    );

    long countByCommentIdAndVote(Long commentId, CommentVoteType vote);

    List<CommentVoteEntity> findAllByCommentIdInAndUserId(Collection<Long> commentIds, Long userId);

    @Query("""
            SELECT v.commentId, v.vote, COUNT(v)
            FROM CommentVoteEntity v
            WHERE v.commentId IN :commentIds
            GROUP BY v.commentId, v.vote
            """)
    List<Object[]> countGroupedByCommentId(@Param("commentIds") Collection<Long> commentIds);
}
