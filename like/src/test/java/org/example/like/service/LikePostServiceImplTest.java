package org.example.like.service;

import com.example.common.dto.PostDto;
import com.example.common.kafka.NotificationKafkaProducer;
import com.example.common.kafka.NotificationType;
import jakarta.persistence.EntityNotFoundException;
import org.example.like.dto.ToggleLikeResponseDto;
import org.example.like.entity.LikePostEntity;
import org.example.like.entity.LikeStatus;
import org.example.like.mapper.LikePostMapper;
import org.example.like.repository.LikePostRepository;
import org.example.like.util.LikePostLookupService;
import org.example.restclient.config.IHttpCore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LikePostServiceImpl — лайк поста")
public class LikePostServiceImplTest {

    @Mock
    private LikePostRepository likeRepository;
    @Mock
    private LikePostMapper likeMapper;
    @Mock
    private LikePostLookupService likePostLookupService;
    @Mock
    private NotificationKafkaProducer notificationProducer;
    @Mock
    private IHttpCore httpCore;

    @InjectMocks
    private LikePostServiceImpl service;

    @BeforeEach
    void urls() {
        ReflectionTestUtils.setField(service, "userBaseUrl", "http://user");
    }

    @Test
    @DisplayName("Лайк удалённого поста — EntityNotFoundException")
    void removedPost() {
        PostDto post = new PostDto();
        post.setStatusPost("REMOVED");
        when(likePostLookupService.getPostDtoFromApi(1L, 2L)).thenReturn(post);

        assertThrows(EntityNotFoundException.class, () -> service.toggleLike(1L, 2L));
    }

    @Test
    @DisplayName("Повторный лайк снимает ACTIVE → NO_ACTIVE и шлёт системный un-like")
    void unlike() {
        PostDto post = new PostDto();
        post.setId(1L);
        post.setAuthorId(9L);
        post.setStatusPost("PUBLISHED");
        post.setContent("Текст поста для сниппета");
        when(likePostLookupService.getPostDtoFromApi(1L, 2L)).thenReturn(post);
        LikePostEntity like = LikePostEntity.builder().postId(1L).userId(2L).likeStatus(LikeStatus.ACTIVE).build();
        when(likeRepository.findByPostIdAndUserIdForUpdate(1L, 2L)).thenReturn(Optional.of(like));
        when(likeRepository.countActiveLikesByPostId(1L)).thenReturn(3L);

        ToggleLikeResponseDto result = service.toggleLike(1L, 2L);

        assertFalse(result.isLiked());
        assertEquals(LikeStatus.NO_ACTIVE, like.getLikeStatus());
        verify(notificationProducer).sendEvent(
                eq(9L), eq(2L), any(), any(),
                eq(NotificationType.POST_LIKE), eq(1L), eq(null),
                eq("__SYSTEM_LIKE_REMOVED__")
        );
    }

    @Test
    @DisplayName("Первый лайк создаёт ACTIVE")
    void firstLike() {
        PostDto post = new PostDto();
        post.setId(1L);
        post.setAuthorId(9L);
        post.setStatusPost("PUBLISHED");
        post.setContent("Пост");
        when(likePostLookupService.getPostDtoFromApi(1L, 2L)).thenReturn(post);
        when(likeRepository.findByPostIdAndUserIdForUpdate(1L, 2L)).thenReturn(Optional.empty());
        when(likeRepository.countActiveLikesByPostId(1L)).thenReturn(1L);

        ToggleLikeResponseDto result = service.toggleLike(1L, 2L);

        assertTrue(result.isLiked());
        verify(likeRepository).saveAndFlush(any(LikePostEntity.class));
    }
}
