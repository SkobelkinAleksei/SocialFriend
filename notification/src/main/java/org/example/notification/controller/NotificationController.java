package org.example.notification.controller;

import com.example.common.kafka.NotificationDto;
import com.example.common.kafka.NotificationGroupDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notification.dto.PushSubscribeRequest;
import org.example.notification.dto.PushViewingRequest;
import org.example.notification.service.NotificationService;
import org.example.notification.service.WebPushService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/social/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    private final WebPushService webPushService;

    @GetMapping
    public ResponseEntity<List<NotificationGroupDto>> getUserNotifications(
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

    @PatchMapping("/mark-group-read")
    public ResponseEntity<Void> markGroupAsRead(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody List<Long> notificationIds
    ) {
        long currentUserId = Long.parseLong(userId);
        log.info("[NotificationController - INFO] Пометка группы из {} уведомлений как прочитанной для пользователя {}",
                notificationIds.size(), currentUserId);
        notificationService.markGroupAsRead(notificationIds, currentUserId);
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

    // Получить количество непрочитанных уведомлений пользователя для колокольчика в UI
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(
            @RequestHeader("X-User-Id") String userId
    ) {
        long currentUserId = Long.parseLong(userId);
        log.info("[NotificationController - INFO] Запрос количества непрочитанных уведомлений для пользователя: {}", currentUserId);

        long unreadCount = notificationService.countUnreadNotifications(currentUserId);
        return ResponseEntity.ok(unreadCount);
    }

    @GetMapping("/push/vapid-public-key")
    public ResponseEntity<Map<String, String>> vapidPublicKey() {
        return webPushService.publicKey()
                .map(key -> ResponseEntity.ok(Map.of("publicKey", key)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/push/subscribe")
    public ResponseEntity<Void> subscribePush(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody PushSubscribeRequest request
    ) {
        webPushService.subscribe(Long.parseLong(userId), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/push/viewing")
    public ResponseEntity<Void> pushViewing(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody PushViewingRequest request
    ) {
        webPushService.setViewing(Long.parseLong(userId), request.getEndpoint(), request.isViewing());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/push/unsubscribe")
    public ResponseEntity<Void> unsubscribePush(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody(required = false) PushSubscribeRequest request
    ) {
        String endpoint = request == null ? null : request.getEndpoint();
        webPushService.unsubscribe(Long.parseLong(userId), endpoint);
        return ResponseEntity.noContent().build();
    }
}
