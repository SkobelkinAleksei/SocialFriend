package org.example.comment.controller;

import lombok.RequiredArgsConstructor;
import org.example.comment.dto.NewCommentDto;
import org.example.comment.service.PrivateCommentService;
import org.springframework.expression.AccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/api/v1/social/comments/private")
@RestController
public class PrivateCommentController {

    private final PrivateCommentService commentService;

    @PutMapping("/{commentId}")
    public ResponseEntity<Void> updateCommentById(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable(name = "commentId") Long commentId,
            @RequestBody NewCommentDto newCommentDto
    ) {
        Long currentUserId = Long.parseLong(userId);
        commentService.updateCommentById(commentId, currentUserId, newCommentDto);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteCommentById(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long commentId
    ) throws AccessException {
        Long currentUserId = Long.parseLong(userId);
        commentService.deleteCommentById(commentId, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
