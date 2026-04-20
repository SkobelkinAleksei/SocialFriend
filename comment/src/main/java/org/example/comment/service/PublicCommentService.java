package org.example.comment.service;

import org.example.comment.dto.CommentDto;
import org.example.comment.dto.NewCommentDto;

import java.util.List;

public interface PublicCommentService {
    void createComment(Long authorId, Long postId, NewCommentDto newCommentDto);
    List<CommentDto> getCommentsByPostId(Long postId);
}
