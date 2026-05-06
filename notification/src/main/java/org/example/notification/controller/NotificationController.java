package org.example.notification.controller;

import com.example.common.kafka.NotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/social/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getUserNotifications(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        long currentUserId = Long.parseLong(userId);
        log.info("[NotificationController - INFO] Запрос уведомлений для пользователя: {}", currentUserId);
        return ResponseEntity.ok(notificationService.getUserNotifications(currentUserId, page, size));
    }

    // Пометить конкретное уведомление как прочитанное
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long notificationId
    ) {
        long currentUserId = Long.parseLong(userId);
        log.info("[NotificationController - INFO] Пометка уведомления {} как прочитанное пользователем {}", notificationId, currentUserId);
        notificationService.markAsRead(notificationId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @RequestHeader("X-User-Id") String userId
    ) {
        long currentUserId = Long.parseLong(userId);
        notificationService.markAllAsRead(currentUserId);
        return ResponseEntity.noContent().build();
    }
}
