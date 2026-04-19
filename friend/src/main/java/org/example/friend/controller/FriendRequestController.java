package org.example.friend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.friend.dto.FriendRequestDto;
import org.example.friend.entity.enums.FriendRequestStatus;
import org.example.friend.entity.enums.ResponseFriendRequest;
import org.example.friend.service.FriendRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/social/friends/requests")
@RestController
public class FriendRequestController {
    private final FriendRequestService friendRequestService;

    @PostMapping("/{addresseeId}")
    public ResponseEntity<Void> addRequestFriend(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable(name = "addresseeId") Long addresseeId
    ) {
        Long requesterId = Long.parseLong(userId);
        friendRequestService.addFriendRequest(requesterId, addresseeId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteRequestFriend(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam Long addresseeId
    ) {
        Long requesterId = Long.parseLong(userId);
        friendRequestService.deleteRequestFriend(requesterId, addresseeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/outgoing")
    public ResponseEntity<List<FriendRequestDto>> getOutgoingRequests(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) FriendRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long currentUserId = Long.parseLong(userId);
        return ResponseEntity.ok().body(
                friendRequestService.getOutgoingRequests(currentUserId, status, page, size)
        );
    }

    @GetMapping("/incoming")
    public ResponseEntity<List<FriendRequestDto>> getIncomingRequests(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long currentUserId = Long.parseLong(userId);
        return ResponseEntity.ok().body(
                friendRequestService.getIncomingRequests(currentUserId, page, size)
        );
    }

    @PutMapping("/{requestId}")
    public ResponseEntity<Void> processFriendRequest(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long requestId,
            @RequestParam ResponseFriendRequest status
    ) throws AccessDeniedException {
        Long currentUserId = Long.parseLong(userId);
        friendRequestService.processFriendRequest(requestId, status, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
