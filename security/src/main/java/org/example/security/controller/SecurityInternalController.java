package org.example.security.controller;

import lombok.RequiredArgsConstructor;
import org.example.security.service.RefreshTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/security")
@RequiredArgsConstructor
public class SecurityInternalController {

    private final RefreshTokenService refreshTokenService;

    @PostMapping("/refresh-tokens/expired")
    public ResponseEntity<Integer> cleanupExpiredRefreshTokens() {
        return ResponseEntity.ok(refreshTokenService.deleteExpiredOrRevoked(500));
    }
}
