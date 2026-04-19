package org.example.friend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.friend.service.PrivateFriendService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/social/friends/private")
@RestController
public class PrivateFriendController {
    private final PrivateFriendService privateFriendService;

    @DeleteMapping
    public ResponseEntity<Void> deleteFriendById(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam Long userId2
    ) {
        Long currentUserId = Long.parseLong(userId);
        privateFriendService.deleteFriendById(currentUserId, userId2);
        return ResponseEntity.noContent().build();
    }
}
