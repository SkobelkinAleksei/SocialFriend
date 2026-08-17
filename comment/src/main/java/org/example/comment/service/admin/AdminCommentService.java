package org.example.comment.service.admin;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.comment.dto.admin.AdminCommentDto;
import org.example.comment.entity.CommentEntity;
import org.example.comment.entity.CommentStatus;
import org.example.comment.repository.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminCommentService {

    private final CommentRepository commentRepository;

    @Transactional(readOnly = true)
    public AdminCommentDto preview(Long commentId) {
        return toDto(require(commentId));
    }

    @Transactional
    public void hideForModeration(Long commentId) {
        CommentEntity comment = require(commentId);
        comment.setCommentStatus(CommentStatus.REMOVED);
    }

    private CommentEntity require(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Комментарий не найден"));
    }

    private static AdminCommentDto toDto(CommentEntity comment) {
        return AdminCommentDto.builder()
                .id(comment.getId())
                .authorId(comment.getAuthorId())
                .postId(comment.getPostId())
                .targetType(comment.getTargetType() == null ? null : comment.getTargetType().name())
                .targetId(comment.getTargetId())
                .content(comment.getContent())
                .status(comment.getCommentStatus() == null ? null : comment.getCommentStatus().name())
                .build();
    }
}
