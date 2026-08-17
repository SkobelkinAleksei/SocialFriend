package org.example.comment.service.impl;

import com.example.common.dto.PostDto;
import jakarta.persistence.EntityNotFoundException;
import org.example.comment.dto.NewCommentDto;
import org.example.comment.entity.CommentEntity;
import org.example.comment.entity.CommentStatus;
import org.example.comment.entity.CommentTargetType;
import org.example.comment.mapper.CommentMapper;
import org.example.comment.repository.CommentRepository;
import org.example.comment.repository.CommentVoteRepository;
import org.example.comment.util.CommentLookupService;
import com.example.common.kafka.NotificationKafkaProducer;
import org.example.restclient.config.IHttpCore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.expression.AccessException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublicCommentServiceImpl — доступ к комментариям и голос")
public class PublicCommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentVoteRepository commentVoteRepository;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private CommentLookupService commentLookupService;
    @Mock
    private NotificationKafkaProducer notificationProducer;
    @Mock
    private IHttpCore httpCore;

    @InjectMocks
    private PublicCommentServiceImpl service;

    @Test
    @DisplayName("Комментарии к посту выключены — IllegalStateException")
    void commentsDisabled() {
        PostDto post = new PostDto();
        post.setId(1L);
        post.setAuthorId(2L);
        post.setCommentsAllowed(false);
        when(commentLookupService.getPostDtoFromApi(1L, 5L)).thenReturn(post);

        assertThrows(IllegalStateException.class,
                () -> service.createComment(5L, 1L, new NewCommentDto("привет сосед", null, null)));
        verify(commentRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Не друг и canComment=false — AccessException")
    void friendsOnly() {
        PostDto post = new PostDto();
        post.setId(1L);
        post.setAuthorId(2L);
        post.setCommentsAllowed(true);
        post.setCanComment(false);
        when(commentLookupService.getPostDtoFromApi(1L, 5L)).thenReturn(post);

        assertThrows(AccessException.class,
                () -> service.createComment(5L, 1L, new NewCommentDto("привет сосед", null, null)));
    }

    @Test
    @DisplayName("Голос не LIKE/DISLIKE — 400-логика IllegalArgumentException")
    void badVote() {
        ReflectionTestUtils.setField(service, "userBaseUrl", "http://user");
        CommentEntity comment = CommentEntity.builder()
                .id(4L).postId(1L).targetId(1L)
                .targetType(CommentTargetType.POST)
                .commentStatus(CommentStatus.PUBLISHED)
                .build();
        when(commentRepository.findById(4L)).thenReturn(Optional.of(comment));

        assertThrows(IllegalArgumentException.class, () -> service.toggleVote(4L, 5L, "LOVE"));
    }

    @Test
    @DisplayName("Голос за снятый комментарий — «не найден»")
    void voteRemoved() {
        CommentEntity comment = CommentEntity.builder()
                .id(4L).commentStatus(CommentStatus.REMOVED).build();
        when(commentRepository.findById(4L)).thenReturn(Optional.of(comment));

        assertThrows(EntityNotFoundException.class, () -> service.toggleVote(4L, 5L, "LIKE"));
    }
}
