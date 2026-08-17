package org.example.post.service.admin;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.post.dto.admin.AdminPostDto;
import org.example.post.entity.PostEntity;
import org.example.post.entity.PostHiddenReason;
import org.example.post.entity.StatusPost;
import org.example.post.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPostService {

    private final PostRepository postRepository;

    @Transactional(readOnly = true)
    public AdminPostDto preview(Long postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Пост не найден"));
        return toDto(post);
    }

    @Transactional
    public void hideForModeration(Long postId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Пост не найден"));
        post.setStatusPost(StatusPost.REMOVED);
        post.setHiddenReason(PostHiddenReason.MODERATION);
        postRepository.save(post);
        log.info("[Admin] Пост {} скрыт модерацией", postId);
    }

    @Transactional
    public void hidePublishedForBan(Long authorId) {
        for (PostEntity post : postRepository.findAllByAuthorIdAndStatusPost(authorId, StatusPost.PUBLISHED)) {
            post.setStatusPost(StatusPost.REMOVED);
            post.setHiddenReason(PostHiddenReason.ACCOUNT_BAN);
            postRepository.save(post);
        }
        log.info("[Admin] Посты автора {} скрыты из‑за бана", authorId);
    }

    @Transactional
    public void restoreHiddenForBan(Long authorId) {
        for (PostEntity post : postRepository.findAllByAuthorIdAndStatusPostAndHiddenReason(
                authorId, StatusPost.REMOVED, PostHiddenReason.ACCOUNT_BAN)) {
            post.setStatusPost(StatusPost.PUBLISHED);
            post.setHiddenReason(null);
            postRepository.save(post);
        }
        log.info("[Admin] Посты автора {} возвращены после разбана", authorId);
    }

    private static AdminPostDto toDto(PostEntity post) {
        return AdminPostDto.builder()
                .id(post.getId())
                .authorId(post.getAuthorId())
                .content(post.getContent())
                .photos(post.getPhotos() == null ? new ArrayList<>() : post.getPhotos())
                .createdAt(post.getCreatedAt())
                .statusPost(post.getStatusPost() == null ? null : post.getStatusPost().name())
                .hiddenReason(post.getHiddenReason() == null ? null : post.getHiddenReason().name())
                .commentsAllowed(post.isCommentsAllowed())
                .build();
    }
}
