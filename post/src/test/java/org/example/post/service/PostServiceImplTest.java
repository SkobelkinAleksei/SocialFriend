package org.example.post.service;

import com.example.common.dto.PostDto;
import jakarta.persistence.EntityNotFoundException;
import org.example.post.dto.NewPostDto;
import org.example.post.dto.UpdatePostDto;
import org.example.post.entity.PostEntity;
import org.example.post.entity.StatusPost;
import org.example.post.exception.ForbiddenException;
import org.example.post.mapper.PostMapper;
import org.example.post.repository.PostRepository;
import org.example.post.repository.PostViewRepository;
import org.example.post.util.PostAccessValidator;
import org.example.post.util.PostLookupService;
import org.example.restclient.config.IHttpCore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostServiceImpl — создание, правка, удаление и скрытие чужих REMOVED")
public class PostServiceImplTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private PostViewRepository postViewRepository;
    @Mock
    private PostMapper postMapper;
    @Mock
    private PostLookupService postLookupService;
    @Mock
    private PostAccessValidator postAccessValidator;
    @Mock
    private IHttpCore httpCore;

    @InjectMocks
    private PostServiceImpl service;

    @Test
    @DisplayName("Создание: автор из gateway, статус PUBLISHED, счётчик просмотров 0")
    void create() {
        ReflectionTestUtils.setField(service, "userBaseUrl", "http://user");
        NewPostDto dto = new NewPostDto("Привет соседи!", true, null);
        PostEntity entity = new PostEntity();
        when(postMapper.toEntity(dto)).thenReturn(entity);
        when(postRepository.save(entity)).thenAnswer(inv -> {
            entity.setId(8L);
            return entity;
        });

        Long id = service.createPost(dto, 3L);

        assertEquals(8L, id);
        assertEquals(3L, entity.getAuthorId());
        assertEquals(StatusPost.PUBLISHED, entity.getStatusPost());
        assertEquals(0L, entity.getViewsCount());
    }

    @Nested
    @DisplayName("Чтение")
    class Read {
        @Test
        @DisplayName("Чужой удалённый пост выглядит как «не найден»")
        void removedHiddenFromOthers() {
            PostEntity post = PostEntity.builder().id(1L).authorId(2L).statusPost(StatusPost.REMOVED).build();
            when(postLookupService.getById(1L)).thenReturn(post);

            assertThrows(EntityNotFoundException.class, () -> service.findPostById(1L, 9L));
        }
    }

    @Nested
    @DisplayName("Права автора")
    class Author {
        @Test
        @DisplayName("Не автор не может править — ForbiddenException")
        void updateForbidden() {
            PostEntity post = PostEntity.builder().id(1L).authorId(2L).statusPost(StatusPost.PUBLISHED).build();
            when(postLookupService.getById(1L)).thenReturn(post);
            doThrow(new ForbiddenException("Пользователь не является автором поста!"))
                    .when(postAccessValidator).validateAuthor(post, 9L);

            assertThrows(ForbiddenException.class, () -> service.updatePost(1L, new UpdatePostDto("новый текст поста", null), 9L));
        }

        @Test
        @DisplayName("Удалённый пост нельзя изменить даже автору")
        void cannotEditRemoved() {
            PostEntity post = PostEntity.builder().id(1L).authorId(2L).statusPost(StatusPost.REMOVED).content("x").build();
            when(postLookupService.getById(1L)).thenReturn(post);

            assertThrows(IllegalArgumentException.class,
                    () -> service.updatePost(1L, new UpdatePostDto("новый текст поста", null), 2L));
        }

        @Test
        @DisplayName("Удаление — мягкое: статус REMOVED, строка не стирается репозиторием")
        void softDelete() {
            PostEntity post = PostEntity.builder().id(1L).authorId(2L).statusPost(StatusPost.PUBLISHED).build();
            when(postLookupService.getById(1L)).thenReturn(post);

            service.deletePost(2L, 1L);

            assertEquals(StatusPost.REMOVED, post.getStatusPost());
            verify(postRepository, never()).delete(any(PostEntity.class));
        }
    }
}
