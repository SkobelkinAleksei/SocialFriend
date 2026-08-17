package org.example.post.service;

import com.example.common.RequestData;
import com.example.common.dto.PostCommentCountDto;
import com.example.common.dto.PostDto;
import com.example.common.dto.PostLikeSummaryDto;
import com.example.common.dto.event.UserDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.example.post.dto.NewPostDto;
import org.example.post.dto.UpdatePostDto;
import org.example.post.entity.PostEntity;
import org.example.post.entity.PostViewEntity;
import org.example.post.entity.StatusPost;
import org.example.post.mapper.PostMapper;
import org.example.post.repository.PostRepository;
import org.example.post.repository.PostViewRepository;
import org.example.post.util.PostAccessValidator;
import org.example.post.util.PostLookupService;
import org.example.post.util.PostStatusSpecification;
import org.example.restclient.config.IHttpCore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostViewRepository postViewRepository;
    private final PostMapper postMapper;
    private final PostLookupService postLookupService;
    private final PostAccessValidator postAccessValidator;
    private final IHttpCore httpCore;

    @Value("${app.services.user-base-url:http://localhost:8081}")
    private String userBaseUrl;

    @Value("${app.services.like-base-url:http://localhost:8085}")
    private String likeBaseUrl;

    @Value("${app.services.comment-base-url:http://localhost:8084}")
    private String commentBaseUrl;

    @Override
    @Transactional(readOnly = true)
    public PostDto findPostById(Long postId, Long currentUserId) {
        log.info("[PostServiceImpl] Получение поста id={} viewer={}", postId, currentUserId);
        PostEntity postEntity = postLookupService.getById(postId);

        if (postEntity.getStatusPost() == StatusPost.REMOVED
                && !postEntity.getAuthorId().equals(currentUserId)) {
            throw new EntityNotFoundException("Такой пост не был найден.");
        }
        if (!postEntity.getAuthorId().equals(currentUserId) && !isAuthorActive(postEntity.getAuthorId(), currentUserId)) {
            throw new EntityNotFoundException("Такой пост не был найден.");
        }

        PostDto dto = toDtoWithPrivacy(postEntity, currentUserId);
        attachEngagement(List.of(dto), currentUserId);
        return dto;
    }

    @Override
    @Transactional
    public Long createPost(NewPostDto newPostDto, Long authorId) {
        log.info("[PostServiceImpl] Создание поста автором {}", authorId);

        PostEntity postEntity = postMapper.toEntity(newPostDto);
        postEntity.setAuthorId(authorId);
        postEntity.setStatusPost(StatusPost.PUBLISHED);
        postEntity.setCommentsAllowed(newPostDto.isCommentsAllowed());
        postEntity.setViewsCount(0L);
        postEntity.setPhotos(normalizePhotos(newPostDto.getPhotos()));
        postRepository.save(postEntity);

        log.info("[PostServiceImpl] Пост создан id={}", postEntity.getId());
        return postEntity.getId();
    }

    @Override
    @Transactional
    public void updatePost(Long postId, UpdatePostDto updatePostDto, Long userId) {
        PostEntity postEntity = postLookupService.getById(postId);
        postAccessValidator.validateAuthor(postEntity, userId);

        if (postEntity.getStatusPost() == StatusPost.REMOVED) {
            throw new IllegalArgumentException("Нельзя изменить удаленный пост.");
        }

        postEntity.setContent(updatePostDto.getContent());
        if (updatePostDto.getPhotos() != null) {
            if (postEntity.getPhotos() == null) {
                postEntity.setPhotos(new ArrayList<>());
            }
            postEntity.getPhotos().clear();
            postEntity.getPhotos().addAll(normalizePhotos(updatePostDto.getPhotos()));
        }
        log.info("[PostServiceImpl] Пост id={} обновлён", postEntity.getId());
    }

    @Override
    @Transactional
    public void deletePost(Long userId, Long postId) {
        PostEntity postEntity = postLookupService.getById(postId);
        postAccessValidator.validateAuthor(postEntity, userId);
        postEntity.setStatusPost(StatusPost.REMOVED);
        postEntity.setHiddenReason(org.example.post.entity.PostHiddenReason.USER);
        log.info("[PostServiceImpl] Пост id={} помечен REMOVED", postId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostDto> findPostsByAuthor(Long authorId, Long currentUserId, int page, int size) {
        if (!authorId.equals(currentUserId) && !isAuthorActive(authorId, currentUserId)) {
            return List.of();
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PostEntity> postEntities =
                postRepository.findAllByAuthorIdAndStatusPost(authorId, StatusPost.PUBLISHED, pageable);

        boolean privacyCanComment = checkCommentPrivacy(authorId, currentUserId);
        List<PostDto> posts = postEntities.stream()
                .map(post -> toDtoWithPrivacy(post, currentUserId, privacyCanComment))
                .toList();
        attachEngagement(posts, currentUserId);
        return posts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostDto> findUserPostsByStatus(
            Long authorId,
            List<StatusPost> status,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Specification<PostEntity> spec = PostStatusSpecification
                .filter(status)
                .and(PostStatusSpecification.byAuthor(authorId));

        Page<PostEntity> postEntities = postRepository.findAll(spec, pageable);
        List<PostDto> posts = postEntities.stream()
                .map(post -> toDtoWithPrivacy(post, authorId, true))
                .toList();
        attachEngagement(posts, authorId);
        return posts;
    }

    @Override
    @Transactional
    public long registerView(Long postId, Long viewerId) {
        PostEntity post = postLookupService.getById(postId);

        if (post.getStatusPost() == StatusPost.REMOVED) {
            throw new EntityNotFoundException("Такой пост не был найден.");
        }

        // Свои просмотры не считаем
        if (post.getAuthorId().equals(viewerId)) {
            return post.getViewsCount();
        }

        if (postViewRepository.existsByPostIdAndUserId(postId, viewerId)) {
            return post.getViewsCount();
        }

        postViewRepository.save(PostViewEntity.builder()
                .postId(postId)
                .userId(viewerId)
                .build());

        post.setViewsCount(post.getViewsCount() + 1);
        postRepository.save(post);
        log.info("[PostServiceImpl] Просмотр поста {} пользователем {} (всего {})",
                postId, viewerId, post.getViewsCount());
        return post.getViewsCount();
    }

    private PostDto toDtoWithPrivacy(PostEntity postEntity, Long viewerId) {
        return toDtoWithPrivacy(postEntity, viewerId, checkCommentPrivacy(postEntity.getAuthorId(), viewerId));
    }

    private PostDto toDtoWithPrivacy(PostEntity postEntity, Long viewerId, boolean privacyCanComment) {
        if (postEntity.getPhotos() != null) {
            Hibernate.initialize(postEntity.getPhotos());
        }
        PostDto dto = postMapper.toDto(postEntity);
        dto.setCanComment(postEntity.isCommentsAllowed() && privacyCanComment);
        if (postEntity.getStatusPost() != null) {
            dto.setStatusPost(postEntity.getStatusPost().name());
        }
        if (dto.getPhotos() == null) {
            dto.setPhotos(new ArrayList<>());
        }
        return dto;
    }

    private void attachEngagement(List<PostDto> posts, Long viewerId) {
        if (posts == null || posts.isEmpty() || viewerId == null) {
            return;
        }
        List<Long> ids = posts.stream()
                .map(PostDto::getId)
                .filter(Objects::nonNull)
                .distinct()
                .limit(100)
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        String joined = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        Map<Long, PostLikeSummaryDto> likes = new HashMap<>();
        try {
            RequestData requestData = new RequestData(
                    likeBaseUrl + "/api/v1/social/likes/summary?postIds=" + joined,
                    Map.of("X-User-Id", String.valueOf(viewerId))
            );
            ResponseEntity<PostLikeSummaryDto[]> response = httpCore.get(requestData, PostLikeSummaryDto[].class);
            if (response != null && response.getBody() != null) {
                for (PostLikeSummaryDto summary : response.getBody()) {
                    if (summary != null && summary.getPostId() != null) {
                        likes.put(summary.getPostId(), summary);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[PostServiceImpl] Не удалось получить сводку лайков: {}", e.getMessage());
        }
        Map<Long, Long> comments = new HashMap<>();
        try {
            RequestData requestData = new RequestData(
                    commentBaseUrl + "/api/v1/social/comments/public/counts?postIds=" + joined,
                    Map.of("X-User-Id", String.valueOf(viewerId))
            );
            ResponseEntity<PostCommentCountDto[]> response = httpCore.get(requestData, PostCommentCountDto[].class);
            if (response != null && response.getBody() != null) {
                for (PostCommentCountDto row : response.getBody()) {
                    if (row != null && row.getPostId() != null) {
                        comments.put(row.getPostId(), row.getCount());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[PostServiceImpl] Не удалось получить счётчики комментариев: {}", e.getMessage());
        }
        for (PostDto dto : posts) {
            PostLikeSummaryDto summary = likes.get(dto.getId());
            dto.setLikesCount(summary != null ? summary.getLikesCount() : 0);
            dto.setLiked(summary != null && summary.isLiked());
            dto.setCommentsCount(comments.getOrDefault(dto.getId(), 0L));
        }
    }

    private static final int MAX_PHOTOS = 10;
    private static final String POST_MEDIA_PREFIX = "/api/v1/social/posts/media/";
    private static final String PHOTO_FILE_PATTERN = "^[a-fA-F0-9\\-]{36}\\.(jpg|jpeg|png|webp|gif)$";

    private List<String> normalizePhotos(List<String> photos) {
        if (photos == null || photos.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> cleaned = photos.stream()
                .filter(url -> url != null && !url.isBlank())
                .map(String::trim)
                .filter(url -> url.startsWith(POST_MEDIA_PREFIX)
                        && url.substring(POST_MEDIA_PREFIX.length()).matches(PHOTO_FILE_PATTERN))
                .distinct()
                .limit(MAX_PHOTOS)
                .toList();
        return new ArrayList<>(cleaned);
    }

    private boolean checkCommentPrivacy(Long authorId, Long viewerId) {
        if (authorId.equals(viewerId)) {
            return true;
        }
        try {
            String userProfileUrl = userBaseUrl + "/api/v1/social/users/" + authorId + "/profile";
            RequestData requestData = new RequestData(
                    userProfileUrl,
                    Map.of("X-User-Id", String.valueOf(viewerId))
            );

            ResponseEntity<UserDto> response = httpCore.get(requestData, UserDto.class);
            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().isCanComment();
            }
        } catch (Exception e) {
            log.error("[Post-Service] Не удалось проверить canComment через user-service", e);
        }
        // fail-closed: при сбое не открываем комментарии всем
        return false;
    }

    private boolean isAuthorActive(Long authorId, Long viewerId) {
        try {
            String userProfileUrl = userBaseUrl + "/api/v1/social/users/" + authorId + "/profile";
            RequestData requestData = new RequestData(
                    userProfileUrl,
                    Map.of("X-User-Id", String.valueOf(viewerId))
            );
            ResponseEntity<UserDto> response = httpCore.get(requestData, UserDto.class);
            if (response != null && response.getBody() != null) {
                String status = response.getBody().getAccountStatus();
                return status == null || "ACTIVE".equalsIgnoreCase(status);
            }
        } catch (Exception e) {
            log.warn("[Post-Service] Не удалось проверить статус автора {}: {}", authorId, e.getMessage());
        }
        return true;
    }
}
