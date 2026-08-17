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
    public void sendEvent(
            Long receiverId,
            Long senderId,
            String senderFirstName,
            String senderLastName,
            NotificationType type,
            Long targetId,
            Long commentId,
            String message
    ) {
        sendEvent(receiverId, senderId, senderFirstName, senderLastName, type, targetId, commentId, message, null);
    }

    @Async
    public void sendEvent(
            Long receiverId,
            Long senderId,
            String senderFirstName,
            String senderLastName,
            NotificationType type,
            Long targetId,
            Long commentId,
            String message,
            String contextLabel
    ) {
        if (receiverId != null && receiverId > 0 && receiverId.equals(senderId)) {
            return;
        }

        NotificationEvent event = NotificationEvent.builder()
                .receiverId(receiverId)
                .senderId(senderId)
                .senderFirstName(senderFirstName)
                .senderLastName(senderLastName)
                .type(type)
                .targetId(targetId)
                .commentId(commentId)
                .message(message)
                .contextLabel(contextLabel)
                .build();

        kafkaTemplate.send("notifications", String.valueOf(receiverId), event);
    }

    @Async
    public void sendEvent(NotificationEvent event) {
        if (event == null || event.getReceiverId() == null) {
            return;
        }
        if (event.getReceiverId() > 0 && event.getReceiverId().equals(event.getSenderId())) {
            return;
        }
        kafkaTemplate.send("notifications", String.valueOf(event.getReceiverId()), event);
    }
}
