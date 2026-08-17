package org.example.friend.controller;

import com.example.common.dto.BlockStatusDto;
import lombok.RequiredArgsConstructor;
import org.example.friend.service.BlockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/social/friends/blocks")
@RequiredArgsConstructor
public class FriendBlockController {

    private final BlockService blockService;

    @PostMapping("/{userId}")
    public ResponseEntity<Void> block(
            @RequestHeader("X-User-Id") Long authorId,
            @PathVariable Long userId
    ) {
        blockService.block(authorId, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> unblock(
            @RequestHeader("X-User-Id") Long authorId,
            @PathVariable Long userId
    ) {
        blockService.unblock(authorId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status/{userId}")
    public ResponseEntity<BlockStatusDto> status(
            @RequestHeader("X-User-Id") Long viewerId,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(blockService.status(viewerId, userId));
    }

    @GetMapping("/check")
    public ResponseEntity<Boolean> checkEitherWay(
            @RequestParam Long userId1,
            @RequestParam Long userId2
    ) {
        return ResponseEntity.ok(blockService.isBlockedEitherWay(userId1, userId2));
    }

    @GetMapping("/hidden-ids")
    public ResponseEntity<java.util.List<Long>> hiddenIds(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(new java.util.ArrayList<>(blockService.hiddenUserIds(userId)));
    }
}
