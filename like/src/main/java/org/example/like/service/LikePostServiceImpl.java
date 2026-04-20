package org.example.like.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.like.dto.LikePostDto;
import org.example.like.entity.LikePostEntity;
import org.example.like.entity.LikeStatus;
import org.example.like.mapper.LikePostMapper;
import org.example.like.repository.LikePostRepository;
import org.example.like.util.LikePostLookupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class LikePostServiceImpl implements LikePostService {
    private final LikePostRepository likeRepository;
    private final LikePostMapper likeMapper;
    private final LikePostLookupService likePostLookupService;

    @Transactional(readOnly = true)
    public List<LikePostDto> getLikesByPostId(Long postId) {
        log.info("[LikePostServiceImpl - INFO] Получение лайков для поста с id: {}", postId);
        List<LikePostEntity> allLikes = likeRepository.findAllActiveByPostId(postId);

        List<LikePostDto> likePostDtos = allLikes.stream()
                .map(likeMapper::toDto)
                .toList();
        log.info("[LikePostServiceImpl - INFO] Для поста с id: {} найдено активных лайков: {}", postId, likePostDtos.size());
        return likePostDtos;
    }

    @Transactional
    public void toggleLike(Long postId, Long userId) {
        likePostLookupService.getPostDtoFromApi(postId);
        log.info("[INFO] Лайк-дизлайк для поста id: {} пользователем id: {}", postId, userId);

        Optional<LikePostEntity> optionalLike = likeRepository.findByPostIdAndUserId(postId, userId);

        if (optionalLike.isPresent()) {
            LikePostEntity likeEntity = optionalLike.get();
            log.info("[LikePostServiceImpl - INFO] Найден существующий лайк id: {} со статусом: {}",
                    likeEntity.getId(), likeEntity.getLikeStatus());

            if (likeEntity.getLikeStatus() == LikeStatus.ACTIVE) {
                likeEntity.setLikeStatus(LikeStatus.NO_ACTIVE);
                log.info("[LikePostServiceImpl - INFO] Лайк id: {} переключен в статус NO_ACTIVE", likeEntity.getId());
            } else {
                likeEntity.setLikeStatus(LikeStatus.ACTIVE);
                log.info("[LikePostServiceImpl - INFO] Лайк id: {} переключен в статус ACTIVE", likeEntity.getId());
            }
            likeRepository.save(likeEntity);
            log.info("[LikePostServiceImpl - INFO] СОХРАНЕНО likeEntity id: {}", likeEntity.getId());
        } else {
            log.info("[LikePostServiceImpl - INFO] Лайк для поста id: {} и пользователя id: {} не найден. Создаем новый.",
                    postId, userId);
            LikePostEntity likePostEntity = new LikePostEntity();

            likePostEntity.setPostId(postId);
            likePostEntity.setUserId(userId);
            likePostEntity.setLikeStatus(LikeStatus.ACTIVE);

            LikePostEntity saved = likeRepository.save(likePostEntity);
            log.info("[LikePostServiceImpl - INFO] СОХРАНЁН НОВЫЙ like id: {}", saved.getId());
        }
    }

    @Transactional(readOnly = true)
    public Long countActiveLikesByPostId(Long postId) {
        return likeRepository.countActiveLikesByPostId(postId);
    }

    public boolean isLikedByUser(Long postId, Long userId) {
        log.info("Проверяем лайк postId={} userId={}", postId, userId);
        boolean result = likeRepository.existsByPostIdAndUserIdAndLikeStatus(postId, userId, LikeStatus.ACTIVE);
        log.info("isLikedByUser postId={} userId={} → {}", postId, userId, result);
        return result;
    }
}
