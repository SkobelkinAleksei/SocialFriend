package org.example.comment.service;

import org.example.comment.dto.CommentDto;
import org.example.comment.dto.CommentVoteDto;
import org.example.comment.dto.NewCommentDto;
import org.springframework.data.domain.Page;
import org.springframework.expression.AccessException;

import com.example.common.dto.PostCommentCountDto;

import java.util.List;

public interface PublicCommentService {
    CommentDto createComment(Long authorId, Long postId, NewCommentDto newCommentDto) throws AccessException;
    Page<CommentDto> getCommentsByPostId(Long postId, int page, int size, Long viewerId);
    long countCommentsByPostId(Long postId, Long viewerId);
    CommentDto createPhotoComment(Long authorId, String kind, Long targetId, NewCommentDto newCommentDto) throws AccessException;
    Page<CommentDto> getPhotoComments(String kind, Long targetId, int page, int size, Long viewerId);
    long countPhotoComments(String kind, Long targetId, Long viewerId);
    CommentVoteDto toggleVote(Long commentId, Long userId, String voteRaw);
    List<PostCommentCountDto> countCommentsByPostIds(List<Long> postIds);
}
