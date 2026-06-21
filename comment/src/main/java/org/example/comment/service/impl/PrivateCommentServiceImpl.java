package org.example.comment.service.impl;

import com.example.common.dto.PostDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.comment.dto.NewCommentDto;
import org.example.comment.entity.CommentEntity;
import org.example.comment.entity.CommentStatus;
import org.example.comment.repository.CommentRepository;
import org.example.comment.service.PrivateCommentService;
import org.example.comment.util.CommentLookupService;
import org.springframework.expression.AccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class PrivateCommentServiceImpl implements PrivateCommentService {
    private final CommentRepository commentRepository;
    private final CommentLookupService commentLookupService;

    @Override
    @Transactional
    public void updateCommentById(
            Long commentId,
            Long authorId,
            NewCommentDto newCommentDto
    ) {
        CommentEntity commentEntity = commentRepository.findById(commentId).orElseThrow(
                () -> new EntityNotFoundException("Комментарий не был найден!"));

        if (!commentEntity.getAuthorId().equals(authorId)) {
            throw new IllegalArgumentException("Нет доступа для изменения комментария!");
        }

        commentEntity.setContent(newCommentDto.getContent());
        log.info("[PublicCommentServiceImpl - INFO] Комментарий был успешно обновлен пользователем {}", authorId);
    }

    @Override
    @Transactional
    public void deleteCommentById(Long commentId, Long currentUserId) throws AccessException {
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Комментарий не найден!"));

        PostDto postDto = commentLookupService.getPostDtoFromApi(comment.getPostId());

        boolean isCommentAuthor = comment.getAuthorId().equals(currentUserId);
        boolean isPostAuthor = postDto.getAuthorId().equals(currentUserId);

        if (!isCommentAuthor && !isPostAuthor) {
            log.warn("[PublicCommentServiceImpl - INFO] Попытка незаконного удаления. User: {}, Comment: {}", currentUserId, commentId);
            throw new AccessException("У вас нет прав на удаление этого комментария!");
        }

        comment.setCommentStatus(CommentStatus.REMOVED);
        log.info("[PublicCommentServiceImpl - INFO] Комментарий {} удален пользователем {}", commentId, currentUserId);
    }
}
