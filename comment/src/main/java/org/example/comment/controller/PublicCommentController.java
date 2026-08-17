package org.example.comment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.example.common.dto.PostCommentCountDto;
import org.example.comment.dto.CommentDto;
import org.example.comment.dto.CommentVoteDto;
import org.example.comment.dto.CommentVoteRequestDto;
import org.example.comment.dto.NewCommentDto;
import org.example.comment.service.PublicCommentService;
import org.springframework.data.domain.Page;
import org.springframework.expression.AccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/v1/social/comments/public")
@RestController
public class PublicCommentController {
    private final PublicCommentService commentService;

    @GetMapping("/counts")
    public ResponseEntity<List<PostCommentCountDto>> countCommentsByPostIds(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam List<Long> postIds
    ) {
        parseUserId(userId);
        return ResponseEntity.ok(commentService.countCommentsByPostIds(postIds));
    }

    @PostMapping("/post/{postId}")
    public ResponseEntity<CommentDto> createComment(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable(name = "postId") Long postId,
            @Valid @RequestBody NewCommentDto newCommentDto
    ) throws AccessException {
        Long currentUserId = parseUserId(userId);
        CommentDto created = commentService.createComment(currentUserId, postId, newCommentDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<Page<CommentDto>> getCommentsByPostId(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable(name = "postId") Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long viewerId = parseUserId(userId);
        return ResponseEntity.ok().body(commentService.getCommentsByPostId(postId, page, size, viewerId));
    }

    @GetMapping("/post/{postId}/count")
    public ResponseEntity<Long> countCommentsByPostId(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long postId
    ) {
        return ResponseEntity.ok(commentService.countCommentsByPostId(postId, parseUserId(userId)));
    }

    @PostMapping("/photo/{kind}/{targetId}")
    public ResponseEntity<CommentDto> createPhotoComment(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String kind,
            @PathVariable Long targetId,
            @Valid @RequestBody NewCommentDto newCommentDto
    ) throws AccessException {
        Long currentUserId = parseUserId(userId);
        CommentDto created = commentService.createPhotoComment(currentUserId, kind, targetId, newCommentDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/photo/{kind}/{targetId}")
    public ResponseEntity<Page<CommentDto>> getPhotoComments(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String kind,
            @PathVariable Long targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long viewerId = parseUserId(userId);
        return ResponseEntity.ok(commentService.getPhotoComments(kind, targetId, page, size, viewerId));
    }

    @GetMapping("/photo/{kind}/{targetId}/count")
    public ResponseEntity<Long> countPhotoComments(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String kind,
            @PathVariable Long targetId
    ) {
        return ResponseEntity.ok(commentService.countPhotoComments(kind, targetId, parseUserId(userId)));
    }

    @PostMapping("/{commentId}/vote")
    public ResponseEntity<CommentVoteDto> toggleVote(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentVoteRequestDto request
    ) {
        Long currentUserId = parseUserId(userId);
        return ResponseEntity.ok(commentService.toggleVote(commentId, currentUserId, request.getVote()));
    }

    private Long parseUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Некорректный заголовок X-User-Id");
        }
        try {
            return Long.parseLong(userId.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректный заголовок X-User-Id");
        }
    }
}
