package org.example.comment.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.example.comment.dto.NewCommentDto;
import org.example.comment.entity.CommentEntity;
import org.example.comment.entity.CommentStatus;
import org.example.comment.entity.CommentTargetType;
import org.example.comment.repository.CommentRepository;
import org.example.comment.util.CommentLookupService;
import com.example.common.kafka.NotificationKafkaProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.expression.AccessException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PrivateCommentServiceImpl — правка и удаление комментария")
public class PrivateCommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentLookupService commentLookupService;
    @Mock
    private NotificationKafkaProducer notificationProducer;

    @InjectMocks
    private PrivateCommentServiceImpl service;

    @Test
    @DisplayName("Чужой комментарий нельзя редактировать")
    void editForeign() {
        CommentEntity comment = CommentEntity.builder()
                .id(1L).authorId(2L).content("hi")
                .commentStatus(CommentStatus.PUBLISHED)
                .targetType(CommentTargetType.POST).postId(9L).targetId(9L)
                .build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThrows(AccessException.class,
                () -> service.updateCommentById(1L, 8L, new NewCommentDto("новый текст", null, null)));
        verify(notificationProducer, never()).sendEvent(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Снятый комментарий при правке выглядит как не найденный")
    void editRemoved() {
        CommentEntity comment = CommentEntity.builder()
                .id(1L).authorId(2L).commentStatus(CommentStatus.REMOVED).build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThrows(EntityNotFoundException.class,
                () -> service.updateCommentById(1L, 2L, new NewCommentDto("новый текст", null, null)));
    }

    @Test
    @DisplayName("Автор может удалить свой комментарий — статус REMOVED")
    void authorDeletes() throws Exception {
        CommentEntity comment = CommentEntity.builder()
                .id(1L).authorId(2L).commentStatus(CommentStatus.PUBLISHED).postId(3L).build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        service.deleteCommentById(1L, 2L);

        assertEquals(CommentStatus.REMOVED, comment.getCommentStatus());
    }

    @Test
    @DisplayName("Повторное удаление уже REMOVED — тихий no-op")
    void deleteAlreadyRemoved() throws Exception {
        CommentEntity comment = CommentEntity.builder()
                .id(1L).authorId(2L).commentStatus(CommentStatus.REMOVED).build();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        service.deleteCommentById(1L, 99L);

        verify(commentLookupService, never()).getPostDtoFromApi(any(), any());
    }
}
