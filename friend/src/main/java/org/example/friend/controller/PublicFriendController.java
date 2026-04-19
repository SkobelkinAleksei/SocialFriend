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
            @PathVariable(name = "userId") Long userId
    ) {
        return ResponseEntity.ok().body(friendService.findAllFriendsByUserId(userId));
    }
}
