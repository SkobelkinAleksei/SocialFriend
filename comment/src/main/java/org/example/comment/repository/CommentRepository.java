package org.example.comment.repository;

import org.example.comment.entity.CommentEntity;
import org.example.comment.entity.CommentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}