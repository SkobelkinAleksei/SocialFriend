package org.example.notification.config;

import com.example.common.kafka.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notification.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = "notifications", groupId = "notification-group")
    public void listen(NotificationEvent event) {
        log.info("[KAFKA] Получено уведомление для пользователя {}: {}",
                event.getReceiverId(), event.getMessage());

        notificationService.processNotification(event);
    }
}
