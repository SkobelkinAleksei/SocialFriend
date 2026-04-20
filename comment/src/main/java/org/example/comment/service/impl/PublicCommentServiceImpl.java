package org.example.comment.service.impl;

import com.example.common.dto.PostDto;
import com.example.common.kafka.NotificationKafkaProducer;
import com.example.common.kafka.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.comment.dto.CommentDto;
import org.example.comment.dto.NewCommentDto;
import org.example.comment.entity.CommentEntity;
import org.example.comment.entity.CommentStatus;
import org.example.comment.mapper.CommentMapper;
import org.example.comment.repository.CommentRepository;
import org.example.comment.service.PublicCommentService;
import org.example.comment.util.CommentLookupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.Objects.isNull;

@Slf4j
@RequiredArgsConstructor
@Service
public class PublicCommentServiceImpl implements PublicCommentService {
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final CommentLookupService commentLookupService;
    private final NotificationKafkaProducer notificationProducer;


    @Override
    @Transactional
    public void createComment(Long authorId, Long postId, NewCommentDto newCommentDto) {
        PostDto postDto = commentLookupService.getPostDtoFromApi(postId);

        if (!postDto.isCommentsAllowed()) {
            log.warn("[PublicCommentServiceImpl - WARN] Попытка оставить комментарий к закрытому посту id: {}", postId);
            throw new IllegalStateException("Автор запретил комментировать этот пост.");

        }

        CommentEntity commentEntity = commentMapper.toEntity(newCommentDto);
        commentEntity.setAuthorId(authorId);
        commentEntity.setPostId(postId);
        commentEntity.setCommentStatus(CommentStatus.PUBLISHED);

        commentRepository.save(commentEntity);
        log.info("[PublicCommentServiceImpl - INFO] Комментарий был успешно создан пользователем {}", authorId);

        notificationProducer.sendEvent(
                postDto.getAuthorId(),
                authorId,
                NotificationType.NEW_COMMENT,
                postId,
                "Оставил комментарий к вашему посту"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsByPostId(Long postId) {

        if (isNull(postId)) {
            log.warn("[PublicCommentServiceImpl - WARN] postId: %s равен null".formatted(postId));
            return List.of();
        }

        List<CommentEntity> allByPostId = commentRepository.findAllByPostIdAndStatusPublished(CommentStatus.PUBLISHED, postId);

        return allByPostId.stream()
                .map(commentMapper::toDto)
                .toList();
    }
}
