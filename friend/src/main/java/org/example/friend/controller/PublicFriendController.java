package org.example.friend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.friend.dto.FriendDto;
import org.example.friend.service.FriendService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/social/friends/public")
@RestController
public class PublicFriendController {
    private final FriendService friendService;

    @GetMapping("/{userId}")
    public ResponseEntity<List<FriendDto>> findAllFriendByUserId(
            @PathVariable(name = "userId") Long userId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok().body(friendService.findAllFriendsByUserId(userId, page, size));
    }

    @GetMapping("/{userId}/count")
    public ResponseEntity<Long> countFriends(@PathVariable(name = "userId") Long userId) {
        return ResponseEntity.ok(friendService.countFriends(userId));
    }

    @GetMapping("/{userId}/ids")
    public ResponseEntity<List<Long>> friendIds(@PathVariable(name = "userId") Long userId) {
        return ResponseEntity.ok(friendService.findFriendIds(userId));
    }

    @GetMapping("/check")
    public ResponseEntity<Boolean> checkFriendship(
            @RequestParam Long userId1,
            @RequestParam Long userId2
    ) {
        log.info("[Friend-Service] Проверка дружбы между {} и {}", userId1, userId2);
        return ResponseEntity.ok(friendService.areFriends(userId1, userId2));
    }
}