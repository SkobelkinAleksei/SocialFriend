package org.example.comment.controller;

import lombok.RequiredArgsConstructor;
import org.example.comment.dto.CommentDto;
import org.example.comment.dto.NewCommentDto;
import org.example.comment.service.PublicCommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/v1/social/comments/public")
@RestController
public class PublicCommentController {
    private final PublicCommentService commentService;

    @PostMapping("/post/{postId}")
    public ResponseEntity<Void> createComment(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable(name = "postId") Long postId,
            @RequestBody NewCommentDto newCommentDto
    ) {
        Long currentUserId = Long.parseLong(userId);
        commentService.createComment(currentUserId, postId, newCommentDto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<CommentDto>> getCommentsByPostId(
            @PathVariable(name = "postId") Long postId
    ) {
        return ResponseEntity.ok().body(commentService.getCommentsByPostId(postId));
    }
}
