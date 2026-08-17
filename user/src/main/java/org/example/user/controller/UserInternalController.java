package org.example.user.controller;

import lombok.RequiredArgsConstructor;
import org.example.user.dto.PhotoCommentTargetDto;
import org.example.user.service.GalleryService;
import org.example.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
public class UserInternalController {

    private final GalleryService galleryService;
    private final UserService userService;

    @GetMapping("/comment-target")
    public ResponseEntity<PhotoCommentTargetDto> commentTarget(
            @RequestHeader("X-User-Id") Long viewerId,
            @RequestParam String kind,
            @RequestParam Long targetId
    ) {
        return ResponseEntity.ok(galleryService.resolveCommentTarget(kind, targetId, viewerId));
    }

    @PostMapping("/{id}/ban")
    public ResponseEntity<Void> ban(@PathVariable Long id) {
        userService.banUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/unban")
    public ResponseEntity<Void> unban(@PathVariable Long id) {
        userService.unbanUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/account-status")
    public ResponseEntity<java.util.Map<String, String>> accountStatus(@PathVariable Long id) {
        return ResponseEntity.ok(java.util.Map.of("status", userService.accountStatus(id)));
    }
}
