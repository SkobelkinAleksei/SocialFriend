package org.example.comment.service.impl;

import com.example.common.dto.PostDto;
import com.example.common.kafka.NotificationKafkaProducer;
import com.example.common.kafka.NotificationType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.comment.dto.NewCommentDto;
import org.example.comment.entity.CommentEntity;
import org.example.comment.entity.CommentStatus;
import org.example.comment.entity.CommentTargetType;
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
    private final NotificationKafkaProducer notificationProducer;

    @Override
    @Transactional
    public void updateCommentById(
            Long commentId,
            Long authorId,
            NewCommentDto newCommentDto
    ) throws AccessException {
        CommentEntity commentEntity = commentRepository.findById(commentId).orElseThrow(
                () -> new EntityNotFoundException("Комментарий не был найден!"));

        if (commentEntity.getCommentStatus() != CommentStatus.PUBLISHED) {
            throw new EntityNotFoundException("Комментарий не был найден!");
        }

        if (!commentEntity.getAuthorId().equals(authorId)) {
            throw new AccessException("Нет доступа для изменения комментария!");
        }

        commentEntity.setContent(newCommentDto.getContent());
        Long targetId = commentEntity.getTargetId() != null ? commentEntity.getTargetId() : commentEntity.getPostId();
        notificationProducer.sendEvent(
                0L,
                authorId,
                "",
                "",
                NotificationType.COMMENT_EDIT,
                targetId,
                commentId,
                newCommentDto.getContent(),
                liveContextLabel(commentEntity)
        );
        log.info("[PrivateCommentServiceImpl] Комментарий {} обновлен пользователем {}", commentId, authorId);
    }

    private String liveContextLabel(CommentEntity comment) {
        CommentTargetType type = comment.getTargetType();
        if (type == CommentTargetType.GALLERY) {
            return "GALLERY:" + comment.getTargetId();
        }
        if (type == CommentTargetType.AVATAR) {
            return "AVATAR:" + comment.getTargetId();
        }
        return "POST";
    }

    @Override
    @Transactional
    public void deleteCommentById(Long commentId, Long currentUserId) throws AccessException {
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Комментарий не найден!"));

        if (comment.getCommentStatus() == CommentStatus.REMOVED) {
            return;
        }

        boolean isCommentAuthor = comment.getAuthorId().equals(currentUserId);
        if (!isCommentAuthor) {
            boolean isOwner = false;
            try {
                if (comment.getPostId() != null) {
                    PostDto postDto = commentLookupService.getPostDtoFromApi(comment.getPostId(), currentUserId);
                    isOwner = postDto.getAuthorId().equals(currentUserId);
                } else if (comment.getTargetType() != null && comment.getTargetId() != null) {
                    var target = commentLookupService.getPhotoCommentTarget(
                            comment.getTargetType().name(), comment.getTargetId(), currentUserId);
                    isOwner = target.getOwnerId().equals(currentUserId);
                }
            } catch (Exception e) {
                log.warn("[PrivateCommentServiceImpl] Не удалось проверить владельца при удалении комментария {}",
                        commentId);
            }
            if (!isOwner) {
                log.warn("[PrivateCommentServiceImpl] Отказ в удалении. User: {}, Comment: {}", currentUserId, commentId);
                throw new AccessException("У вас нет прав на удаление этого комментария!");
            }
        }

        comment.setCommentStatus(CommentStatus.REMOVED);
        log.info("[PrivateCommentServiceImpl] Комментарий {} удален пользователем {}", commentId, currentUserId);
    }
}
