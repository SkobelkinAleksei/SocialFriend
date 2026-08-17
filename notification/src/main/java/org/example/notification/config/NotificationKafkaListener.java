package org.example.notification.config;

import com.example.common.dto.CommentWsDto;
import com.example.common.kafka.NotificationEvent;
import com.example.common.kafka.NotificationType;
import com.example.common.kafka.UserRegisteredEvent;
import com.example.common.kafka.UserSettingsUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notification.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaListener {

    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(
            topics = "notifications",
            groupId = "notification-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listen(NotificationEvent event) {
        log.info("[NotificationKafkaListener - INFO] Получено событие из Kafka для пользователя {}: {}",
                event.getReceiverId(), event.getMessage());

        if (event.getType() == NotificationType.COMMENT_VOTE) {
            broadcastCommentVote(event);
            return;
        }
        if (event.getType() == NotificationType.COMMENT_EDIT) {
            broadcastCommentEdit(event);
            return;
        }

        boolean liveOnly = event.getReceiverId() != null && event.getReceiverId() <= 0;
        if (liveOnly && (event.getType() == NotificationType.NEW_COMMENT || event.getType() == NotificationType.COMMENT_REPLY)) {
            broadcastLiveComment(event);
            return;
        }

        notificationService.processNotification(event);
    }

    private void broadcastLiveComment(NotificationEvent event) {
        try {
            CommentWsDto wsComment = CommentWsDto.builder()
                    .id(event.getCommentId())
                    .authorId(event.getSenderId())
                    .postId(event.getTargetId())
                    .content(event.getMessage())
                    .authorFirstName(event.getSenderFirstName())
                    .authorLastName(event.getSenderLastName())
                    .authorAvatarUrl(event.getAuthorAvatarUrl())
                    .replyToUserId(event.getReplyToUserId())
                    .replyToAuthorName(event.getReplyToAuthorName())
                    .eventType("COMMENT")
                    .likesCount(0L)
                    .dislikesCount(0L)
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
            sendCommentWs(event.getContextLabel(), event.getTargetId(), wsComment);
        } catch (Exception wsEx) {
            log.error("[WebSocket-ERROR] Не удалось оттранслировать живой комментарий: ", wsEx);
        }
    }

    private void broadcastCommentEdit(NotificationEvent event) {
        try {
            CommentWsDto ws = CommentWsDto.builder()
                    .id(event.getCommentId())
                    .postId(event.getTargetId())
                    .content(event.getMessage())
                    .eventType("EDIT")
                    .build();
            sendCommentWs(event.getContextLabel(), event.getTargetId(), ws);
        } catch (Exception wsEx) {
            log.error("[WebSocket-ERROR] Не удалось оттранслировать правку комментария: ", wsEx);
        }
    }

    private void broadcastCommentVote(NotificationEvent event) {
        try {
            String[] parts = event.getMessage() == null ? new String[0] : event.getMessage().split(":");
            long likes = parts.length > 0 ? parseCount(parts[0]) : 0L;
            long dislikes = parts.length > 1 ? parseCount(parts[1]) : 0L;
            CommentWsDto ws = CommentWsDto.builder()
                    .id(event.getCommentId())
                    .postId(event.getTargetId())
                    .eventType("VOTE")
                    .likesCount(likes)
                    .dislikesCount(dislikes)
                    .build();
            sendCommentWs(event.getContextLabel(), event.getTargetId(), ws);
        } catch (Exception wsEx) {
            log.error("[WebSocket-ERROR] Не удалось оттранслировать голос комментария: ", wsEx);
        }
    }

    private void sendCommentWs(String contextLabel, Long targetId, CommentWsDto payload) {
        String label = contextLabel == null ? "POST" : contextLabel;
        if (label.startsWith("GALLERY:") || label.startsWith("AVATAR:")) {
            String kind = label.startsWith("GALLERY:") ? "GALLERY" : "AVATAR";
            messagingTemplate.convertAndSend("/topic/photos/" + kind + "/" + targetId + "/comments", payload);
        } else {
            messagingTemplate.convertAndSend("/topic/posts/" + targetId + "/comments", payload);
        }
    }

    private long parseCount(String raw) {
        try {
            return Long.parseLong(raw.trim());
        } catch (Exception e) {
            return 0L;
        }
    }

    @KafkaListener(
            topics = "user-settings-updated-topic",
            groupId = "notification-settings-group",
            containerFactory = "settingsKafkaListenerContainerFactory"
    )
    public void handleSettingsUpdate(UserSettingsUpdatedEvent event) {
        log.info("[KafkaListener] Получено событие обновления настроек для ID: {}. Передаем в сервис.", event.getUserId());
        notificationService.updateLocalUserSettings(event);
    }

    @KafkaListener(
            topics = "user-registered",
            groupId = "notification-registration-group",
            containerFactory = "registrationKafkaListenerContainerFactory"
    )
    public void handleNewUserRegistration(UserRegisteredEvent event) {
        log.info("[KafkaListener] Поймали событие регистрации нового пользователя с ID: {}. Создаем профиль настроек.", event.getId());
        notificationService.initDefaultSettingsForNewUser(event.getId());
    }
}
