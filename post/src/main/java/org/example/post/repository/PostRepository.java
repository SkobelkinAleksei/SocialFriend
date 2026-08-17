package org.example.post.repository;

import org.example.post.entity.StatusPost;
import org.example.post.entity.PostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<PostEntity, Long>, JpaSpecificationExecutor<PostEntity> {

    Page<PostEntity> findAllByAuthorIdAndStatusPost(Long authorId, StatusPost statusPost, Pageable pageable);

    java.util.List<PostEntity> findAllByAuthorIdAndStatusPost(Long authorId, StatusPost statusPost);

    java.util.List<PostEntity> findAllByAuthorIdAndStatusPostAndHiddenReason(
            Long authorId,
            StatusPost statusPost,
            org.example.post.entity.PostHiddenReason hiddenReason
    );
}
