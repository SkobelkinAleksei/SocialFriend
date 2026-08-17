package org.example.user.controller;

import com.example.common.dto.UserSettingsDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.user.service.UserSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/social/users/profile/settings")
@RequiredArgsConstructor
@Slf4j
public class UserSettingsController {

    private final UserSettingsService settingsService;

    @GetMapping
    public ResponseEntity<UserSettingsDto> getMySettings(
            @RequestHeader("X-User-Id") Long userId
    ) {
        log.info("[UserSettingsController] Получен GET-запрос настроек от пользователя с ID: {}", userId);
        UserSettingsDto dto = settingsService.getSettings(userId);
        return ResponseEntity.ok(dto);
    }

    @PutMapping
    public ResponseEntity<Void> updateMySettings(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UserSettingsDto settingsDto
    ) {
        log.info("[UserSettingsController] Получен PUT-запрос на обновление настроек от пользователя с ID: {}", userId);
        settingsService.updateSettings(userId, settingsDto);
        return ResponseEntity.ok().build();
    }
}
