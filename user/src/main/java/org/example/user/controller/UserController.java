package org.example.user.controller;

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

    @GetMapping("/me")
    public ResponseEntity<UserFullDto> getMyProfile(
            @RequestHeader("X-User-Id") String userId
    ) {
        Long currentUserId = Long.parseLong(userId);
        log.info("[UserController - INFO] Пришел запрос на получение профиля пользователя с id: {}", currentUserId);
        return ResponseEntity.ok().body(userService.getMyProfile(currentUserId));
    }

//    @GetMapping("/post")
//    public ResponseEntity<UserDto> getUserById(
//            @RequestHeader("X-User-Id") String userId
//    ) {
//        Long currentUserId = Long.parseLong(userId);
//        log.info("[INFO] Пришел запрос на получение пользователя по id: {}", currentUserId);
//        return ResponseEntity.ok().body(userService.getUserById(currentUserId));
//    }
//
//    @GetMapping("/public/{userId}")
//    public ResponseEntity<UserDto> getUserPublic(@PathVariable Long userId) {
//        log.info("[INFO] Пришел запрос на получение пользователя по id={} для друзей", userId);
//        return ResponseEntity.ok().body(userService.getUserById(userId));
//    }

    @GetMapping("/search/by-email")
    public ResponseEntity<UserDto> getUserByEmail(
            @RequestParam String email
    ) {
        log.info("[UserController - INFO] Пришел запрос на поиск пользователя по email: {}", email);
        return ResponseEntity.ok().body(userService.getUserByEmail(email));
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserDto>> search(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody(required = false) UserFilterDto filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("[UserController - INFO] Пришел запрос на поиск пользователей по фильтру: {}, страница: {}, размер: {}",
                filter, page, size);
        Long currentUserId = Long.parseLong(userId);
        return ResponseEntity.ok().body(userService.searchUsers(currentUserId, filter, page, size));
    }

    @PutMapping("/account/update")
    public ResponseEntity<UserDto> updateUserAccount(
            @Valid @RequestBody UpdateUserDto updateAccountUser,
            @RequestHeader("X-User-Id") String userId
    ) {
        Long currentUserId = Long.parseLong(userId);
        log.info("[UserController - INFO] Пришел запрос на обновление аккаунта пользователя с id: {}", currentUserId);
        return ResponseEntity.ok().body(userService.updateUserAccount(currentUserId, updateAccountUser));
    }

    @PutMapping("/account/update/pass")
    public ResponseEntity<Void> updatePasswordUser(
            @Valid @RequestBody UpdatePasswordUserDto updatePasswordUserDto,
            @RequestHeader("X-User-Id") String userId
    ) {
        Long currentUserId = Long.parseLong(userId);
        log.info("[UserController - INFO] Пришел запрос на обновление пароля пользователя с id: {}", currentUserId);
        userService.updatePassword(currentUserId, updatePasswordUserDto);
        return ResponseEntity.noContent().build();
    }
}
