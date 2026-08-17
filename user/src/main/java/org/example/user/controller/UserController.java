package org.example.user.controller;

import com.example.common.dto.event.UserDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.user.dto.*;
import org.example.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/social/users")
@RestController
public class UserController {
    private final UserService userService;

    @PostMapping("/me/presence")
    public ResponseEntity<Void> heartbeat(@RequestHeader("X-User-Id") String userId) {
        userService.heartbeat(Long.parseLong(userId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/presence")
    public ResponseEntity<java.util.Map<Long, UserPresenceDto>> getPresence(
            @RequestParam(required = false) List<Long> ids
    ) {
        return ResponseEntity.ok(userService.getPresence(ids == null ? List.of() : ids));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String currentUserId
    ) {
        // Header optional: gateway ставит его для UI; internal-вызовы (friend и др.) могут ходить без него
        log.info("[UserController - INFO] Пришел запрос на получение профиля пользователя по id: {} от пользователя: {}",
                id, currentUserId);
        return ResponseEntity.ok().body(userService.getUserById(id));
    }

    @GetMapping("/me")
    public ResponseEntity<UserFullDto> getMyProfile(
            @RequestHeader("X-User-Id") String userId
    ) {
        Long currentUserId = Long.parseLong(userId);
        log.info("[UserController - INFO] Пришел запрос на получение профиля пользователя с id: {}", currentUserId);
        return ResponseEntity.ok().body(userService.getMyProfile(currentUserId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserDto> updateUserAccount(
            @Valid @RequestBody UpdateUserDto updateAccountUser,
            @RequestHeader("X-User-Id") String userId
    ) {
        Long currentUserId = Long.parseLong(userId);
        log.info("[UserController - INFO] Пришел запрос на обновление аккаунта пользователя с id: {}", currentUserId);
        return ResponseEntity.ok().body(userService.updateUserAccount(currentUserId, updateAccountUser));
    }

    @PutMapping("/me/pass")
    public ResponseEntity<Void> updatePasswordUser(
            @Valid @RequestBody UpdatePasswordUserDto updatePasswordUserDto,
            @RequestHeader("X-User-Id") String userId
    ) {
        Long currentUserId = Long.parseLong(userId);
        log.info("[UserController - INFO] Пришел запрос на обновление пароля пользователя с id: {}", currentUserId);
        userService.updatePassword(currentUserId, updatePasswordUserDto);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/status")
    public ResponseEntity<Void> updateMyBio(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody(required = false) UpdateBioDto newBio
    ) {
        userService.updateBio(userId, newBio);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search/by-email")
    public ResponseEntity<UserDto> searchUserByEmail(
            @RequestParam String email
    ) {
        log.info("[UserController - INFO] Пришел запрос на поиск пользователя по email: {}", email);
        return ResponseEntity.ok().body(userService.searchUserByEmail(email));
    }

    @GetMapping("/search/by-ids")
    public ResponseEntity<List<UserDto>> getUsersByIds(@RequestParam List<Long> ids) {
        log.info("[UserController] Внутренний пакетный запрос пользователей по ids: {}", ids);
        return ResponseEntity.ok().body(userService.getUsersByIds(ids));
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserDto>> search(
            @RequestHeader("X-User-Id") String userId,
            UserFilterDto filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("[UserController - INFO] Пришел запрос на поиск пользователей по фильтру: {}, страница: {}, размер: {}",
                filter, page, size);
        Long currentUserId = Long.parseLong(userId);
        return ResponseEntity.ok().body(userService.searchUsers(currentUserId, filter, page, size));
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserDto> getUserProfileForUi(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long viewerId // ID того, кто смотрит
    ) {
        return ResponseEntity.ok(userService.getUserProfileForViewer(userId, viewerId));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(@RequestHeader("X-User-Id") String userId) {
        userService.deleteMyAccount(Long.parseLong(userId));
        return ResponseEntity.noContent().build();
    }
}
