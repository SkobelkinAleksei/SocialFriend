package org.example.like.service;

import com.example.common.RequestData;
import com.example.common.dto.PostDto;
import com.example.common.dto.PostLikeSummaryDto;
import com.example.common.dto.event.UserDto;
import com.example.common.kafka.NotificationKafkaProducer;
import com.example.common.kafka.NotificationType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.like.dto.LikePostDto;
import org.example.like.dto.ToggleLikeResponseDto;
import org.example.like.entity.LikePostEntity;
import org.example.like.entity.LikeStatus;
import org.example.like.mapper.LikePostMapper;
import org.example.like.repository.LikePostRepository;
import org.example.like.util.LikePostLookupService;
import org.example.restclient.config.IHttpCore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class LikePostServiceImpl implements LikePostService {
    private final LikePostRepository likeRepository;
    private final LikePostMapper likeMapper;
    private final LikePostLookupService likePostLookupService;
    private final NotificationKafkaProducer notificationProducer;
    private final IHttpCore httpCore;

    @Value("${app.services.user-base-url:http://localhost:8081}")
    private String userBaseUrl;

    @Override
    @Transactional(readOnly = true)
    public List<LikePostDto> getLikesByPostId(Long postId, Long viewerId) {
        assertPostVisible(postId, viewerId);
        log.info("[LikePostServiceImpl] Получение лайков для поста id={}", postId);
        return likeRepository.findAllActiveByPostId(postId).stream()
                .map(likeMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ToggleLikeResponseDto toggleLike(Long postId, Long userId) {
        PostDto postDto = likePostLookupService.getPostDtoFromApi(postId, userId);

        if ("REMOVED".equalsIgnoreCase(postDto.getStatusPost())) {
            throw new EntityNotFoundException("Такой пост не был найден.");
        }

        log.info("[LikePostServiceImpl] Toggle лайка postId={} userId={}", postId, userId);

        String likerFirstName = "Житель";
        String likerLastName = "Района";
        try {
            String userUrl = userBaseUrl + "/api/v1/social/users/" + userId;
            ResponseEntity<UserDto> response = httpCore.get(new RequestData(userUrl), UserDto.class);
            if (response != null && response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                likerFirstName = response.getBody().getFirstName();
                likerLastName = response.getBody().getLastName();
            }
        } catch (Exception e) {
            log.error("[LikePostServiceImpl] Не удалось подтянуть ФИО пользователя {}", userId, e);
        }

        boolean liked;
        try {
            Optional<LikePostEntity> optionalLike = likeRepository.findByPostIdAndUserIdForUpdate(postId, userId);
            if (optionalLike.isPresent()) {
                LikePostEntity likeEntity = optionalLike.get();
                if (likeEntity.getLikeStatus() == LikeStatus.ACTIVE) {
                    likeEntity.setLikeStatus(LikeStatus.NO_ACTIVE);
                    likeRepository.save(likeEntity);
                    liked = false;
                } else {
                    likeEntity.setLikeStatus(LikeStatus.ACTIVE);
                    likeRepository.save(likeEntity);
                    liked = true;
                }
            } else {
                LikePostEntity likePostEntity = LikePostEntity.builder()
                        .postId(postId)
                        .userId(userId)
                        .likeStatus(LikeStatus.ACTIVE)
                        .build();
                likeRepository.saveAndFlush(likePostEntity);
                liked = true;
            }
        } catch (DataIntegrityViolationException e) {
            log.warn("[LikePostServiceImpl] Конфликт уникальности лайка postId={} userId={}, повтор", postId, userId);
            LikePostEntity likeEntity = likeRepository.findByPostIdAndUserIdForUpdate(postId, userId)
                    .orElseThrow(() -> e);
            if (likeEntity.getLikeStatus() == LikeStatus.ACTIVE) {
                likeEntity.setLikeStatus(LikeStatus.NO_ACTIVE);
                liked = false;
            } else {
                likeEntity.setLikeStatus(LikeStatus.ACTIVE);
                liked = true;
            }
            likeRepository.save(likeEntity);
        }

        long likesCount = likeRepository.countActiveLikesByPostId(postId);

        String message = liked ? buildLikeMessage(postDto.getContent()) : "__SYSTEM_LIKE_REMOVED__";
        log.info("[LikePostServiceImpl] Уведомление лайка postId={} liked={} message={}", postId, liked, message);
        String firstName = likerFirstName;
        String lastName = likerLastName;
        Long authorId = postDto.getAuthorId();

        Runnable notify = () -> notificationProducer.sendEvent(
                authorId,
                userId,
                firstName,
                lastName,
                NotificationType.POST_LIKE,
                postId,
                null,
                message
        );

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notify.run();
                }
            });
        } else {
            notify.run();
        }

        return ToggleLikeResponseDto.builder()
                .liked(liked)
                .likesCount(likesCount)
                .build();
    }

    private static String buildLikeMessage(String content) {
        String trimmed = content == null ? "" : content.trim().replaceAll("\\s+", " ");
        if (trimmed.isEmpty()) {
            return "оценил(а) ваш пост";
        }
        final int max = 80;
        String snippet = trimmed.length() <= max
                ? trimmed
                : trimmed.substring(0, max).trim() + "...";
        return "оценил(а) ваш пост: \"" + snippet + "\"";
    }

    @Override
    @Transactional(readOnly = true)
    public Long countActiveLikesByPostId(Long postId, Long viewerId) {
        assertPostVisible(postId, viewerId);
        return likeRepository.countActiveLikesByPostId(postId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLikedByUser(Long postId, Long userId) {
        assertPostVisible(postId, userId);
        return likeRepository.existsByPostIdAndUserIdAndLikeStatus(postId, userId, LikeStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostLikeSummaryDto> getSummaryByPostIds(List<Long> postIds, Long userId) {
        if (postIds == null || postIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = postIds.stream().filter(java.util.Objects::nonNull).distinct().limit(100).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        java.util.Map<Long, Long> counts = new java.util.HashMap<>();
        for (Object[] row : likeRepository.countActiveLikesGroupedByPostId(ids, LikeStatus.ACTIVE)) {
            if (row == null || row[0] == null) {
                continue;
            }
            counts.put((Long) row[0], ((Number) row[1]).longValue());
        }
        java.util.Set<Long> liked = new java.util.HashSet<>(
                likeRepository.findActiveLikedPostIds(userId, ids, LikeStatus.ACTIVE)
        );
        return ids.stream()
                .map(id -> new PostLikeSummaryDto(id, counts.getOrDefault(id, 0L), liked.contains(id)))
                .toList();
    }

    private void assertPostVisible(Long postId, Long viewerId) {
        PostDto postDto = likePostLookupService.getPostDtoFromApi(postId, viewerId);
        if ("REMOVED".equalsIgnoreCase(postDto.getStatusPost())) {
            throw new EntityNotFoundException("Такой пост не был найден.");
        }
    }
}
