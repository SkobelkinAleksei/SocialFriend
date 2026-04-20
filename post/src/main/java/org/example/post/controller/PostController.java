package org.example.post.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.post.dto.NewPostDto;
import org.example.post.dto.PostDto;
import org.example.post.dto.UpdatePostDto;
import org.example.post.entity.StatusPost;
import org.example.post.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/social/posts")
@RestController
public class PostController {
    private final PostService postService;

    @GetMapping("/id/{postId}")
    public ResponseEntity<PostDto> findPostById(
            @PathVariable(name = "postId") Long postId
    ) {
        log.info("[PostController - INFO] Пришел запрос на получение поста с id: {}", postId);
        return ResponseEntity.ok().body(postService.findPostById(postId));
    }

    @PostMapping
    public ResponseEntity<Long> createPost(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody NewPostDto newPostDto
    ) {
        Long currentUserId = Long.parseLong(userId);
        log.info("[PostController - INFO] Пришел запрос на создание нового поста от автора с id: {}", currentUserId);
        return ResponseEntity.ok().body(
                postService.createPost(newPostDto, currentUserId)
        );
    }

    @PutMapping("/{postId}")
    public ResponseEntity<Void> updatePost(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long postId,
            @RequestBody UpdatePostDto updatePostDto
    ) {
        Long currentUserId = Long.parseLong(userId);
        log.info("[PostController - INFO] Пришел запрос на обновление поста с id: {} пользователем с id: {}", postId, currentUserId);
        postService.updatePost(postId, updatePostDto, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostDto>> findPostsByAuthor(
            @PathVariable(name = "userId") Long userId
    ) {
        log.info("[PostController - INFO] Пришел запрос на получение постов пользователя с id: {}", userId);
        return ResponseEntity.ok().body(postService.findPostsByAuthor(userId));
    }

    @GetMapping("/my-posts")
    public ResponseEntity<List<PostDto>> findUserPostsByStatus(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) List<StatusPost> status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long currentUserId = Long.parseLong(userId);
        log.info("[PostController - INFO] Пришел запрос на поиск постов автора id: {} по статусам: {}, страница: {}, размер: {}",
                currentUserId, status, page, size);
        return ResponseEntity.ok().body(postService.findUserPostsByStatus(currentUserId, status, page, size));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable(name = "postId") Long postId
    ) {
        Long currentUserId = Long.parseLong(userId);
        log.info("[PostController - INFO] Пришел запрос удаление поста с id: {} пользователем с id: {}", postId, currentUserId);
        postService.deletePost(currentUserId, postId);
        return ResponseEntity.noContent().build();
    }
}
