package com.example.common.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    public void sendEvent(Long receiverId, Long senderId, NotificationType type, Long targetId, String message) {
        // Базовая проверка: не уведомляем самого себя
        if (receiverId != null && receiverId.equals(senderId)) {
            return;
        }

        NotificationEvent event = NotificationEvent.builder()
                .receiverId(receiverId)
                .senderId(senderId)
                .type(type)
                .targetId(targetId)
                .message(message)
                .build();

        log.info("[NotificationKafkaProducer - INFO] Отправка уведомления типа {} для пользователя {}", type, receiverId);
        kafkaTemplate.send("notifications", event);
    }
}
