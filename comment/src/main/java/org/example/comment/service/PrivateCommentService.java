package org.example.comment.service;

import org.example.comment.dto.NewCommentDto;
import org.springframework.expression.AccessException;

public interface PrivateCommentService {
    void updateCommentById(Long commentId, Long authorId, NewCommentDto newCommentDto);
    void deleteCommentById(Long commentId, Long currentUserId) throws AccessException;
}
