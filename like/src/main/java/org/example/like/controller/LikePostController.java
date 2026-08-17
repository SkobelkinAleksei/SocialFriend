package org.example.like.controller;

import com.example.common.dto.PostLikeSummaryDto;
import lombok.RequiredArgsConstructor;
import org.example.like.dto.LikePostDto;
import org.example.like.dto.ToggleLikeResponseDto;
import org.example.like.service.LikePostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/v1/social/likes")
@RestController
public class LikePostController {
    private final LikePostService likeService;

    @GetMapping("/summary")
    public ResponseEntity<List<PostLikeSummaryDto>> getSummaryByPostIds(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam List<Long> postIds
    ) {
        return ResponseEntity.ok(likeService.getSummaryByPostIds(postIds, parseUserId(userId)));
    }

    @GetMapping("/{postId}/list-like")
    public ResponseEntity<List<LikePostDto>> getLikesByPostId(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable(name = "postId") Long postId
    ) {
        return ResponseEntity.ok().body(likeService.getLikesByPostId(postId, parseUserId(userId)));
    }

    @GetMapping("/{postId}/is-liked")
    public ResponseEntity<Boolean> isLikedByUser(
            @PathVariable Long postId,
            @RequestHeader("X-User-Id") String userId
    ) {
        Long currentUserId = parseUserId(userId);
        return ResponseEntity.ok(likeService.isLikedByUser(postId, currentUserId));
    }

    @GetMapping("/{postId}/likes-count")
    public ResponseEntity<Long> getLikesCount(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long postId
    ) {
        return ResponseEntity.ok(likeService.countActiveLikesByPostId(postId, parseUserId(userId)));
    }

    @PostMapping("/{postId}")
    public ResponseEntity<ToggleLikeResponseDto> toggleLike(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable(name = "postId") Long postId
    ) {
        Long currentUserId = parseUserId(userId);
        return ResponseEntity.ok(likeService.toggleLike(postId, currentUserId));
    }

    private long parseUserId(String userId) {
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
