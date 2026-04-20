package org.example.like.controller;

import lombok.RequiredArgsConstructor;
import org.example.like.dto.LikePostDto;
import org.example.like.service.LikePostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/v1/social/likes")
@RestController
public class LikePostController {
    private final LikePostService likeService;

    @GetMapping("/{postId}/list-like")
    public ResponseEntity<List<LikePostDto>> getLikesByPostId(
            @PathVariable(name = "postId") Long postId
    ) {
        return ResponseEntity.ok().body(likeService.getLikesByPostId(postId));
    }

    @GetMapping("/{postId}/is-liked")
    public ResponseEntity<Boolean> isLikedByUser(
            @PathVariable Long postId,
            @RequestHeader("X-User-Id") String userId
    ) {
        Long currentUserId = Long.parseLong(userId);
        boolean isLiked = likeService.isLikedByUser(postId, currentUserId);
        return ResponseEntity.ok(isLiked);
    }

    @GetMapping("/{postId}/likes-count")
    public ResponseEntity<Long> getLikesCount(
            @PathVariable Long postId
    ) {
        return ResponseEntity.ok(likeService.countActiveLikesByPostId(postId));
    }

    @PostMapping("/{postId}")
    public ResponseEntity<Void> toggleLike(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable(name = "postId") Long postId
    ) {
        Long currentUserId = Long.parseLong(userId);
        likeService.toggleLike(postId, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
