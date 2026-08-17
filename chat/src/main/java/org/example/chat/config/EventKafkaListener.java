package org.example.chat.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.common.kafka.EventCreatedEvent;
import com.example.common.kafka.EventUserJoinedEvent;
import com.example.common.kafka.EventUserLeftEvent;
import org.example.chat.service.GroupChatService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class EventKafkaListener {

    private final GroupChatService groupChatService;

    // 1. Слушаем топик создания нового события
    @KafkaListener(
            topics = "event-created-topic",
            groupId = "chat-group-social",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleEventCreated(EventCreatedEvent event) {
        log.info("[Kafka Consumer] Получено событие создания встречи. EventID: {}, Название: '{}', Создатель: {}",
                event.getEventId(), event.getTitle(), event.getOwnerId());
        try {
            groupChatService.createGroupRoom(event.getEventId(), event.getTitle(), event.getOwnerId());
            log.info("[Kafka Consumer - SUCCESS] Групповой чат для события {} успешно создан", event.getEventId());
        } catch (Exception e) {
            log.error("[Kafka Consumer - ERROR] Не удалось автоматически создать чат для события {}: {}",
                    event.getEventId(), e.getMessage());
            throw e;
        }
    }

    // 2. Слушаем топик вступления / одобрения пользователя
    @KafkaListener(
            topics = "event-user-joined-topic",
            groupId = "chat-group-social",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleUserJoined(EventUserJoinedEvent event) {
        log.info("[Kafka Consumer] Получено событие вступления пользователя {} в событие {}", event.getUserId(), event.getEventId());
        try {
            // Вызываем правильный метод сервиса, передавая туда имена
            groupChatService.joinToGroupRoomAndNotify(
                    event.getEventId(),
                    event.getUserId(),
                    event.getFirstName(),
                    event.getLastName()
            );
            log.info("[Kafka Consumer - SUCCESS] Пользователь успешно обработан сервисом чатов");
        } catch (Exception e) {
            log.error("[Kafka Consumer - ERROR] Ошибка сервиса при обработке вступления: {}", e.getMessage());
            throw e;
        }
    }

    // Слушаем топик выхода / отказа от участия / принудительного удаления
    @KafkaListener(
            topics = "event-user-left-topic",
            groupId = "chat-group-social",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleUserLeft(EventUserLeftEvent event) {
        log.info("[Kafka Consumer] Получено событие ВЫХОДА/КИКА пользователя. eventId={}, userId={}, status={}, kicked={}",
                event.getEventId(), event.getUserId(), event.getStatus(), event.isKicked());
        try {
            Long eventId = event.getEventId();
            Long userId = event.getUserId();
            String status = event.getStatus() == null ? "" : event.getStatus();
            boolean isKicked = event.isKicked();
            String firstName = event.getFirstName() != null ? event.getFirstName() : "Участник";
            String lastName = event.getLastName() != null ? event.getLastName() : "";

            if (isKicked) {
                log.info("[Kafka Consumer] Обнаружен принудительный КИК юзера {} из события {}. Запуск тотального удаления из чата.", userId, eventId);
                groupChatService.kickParticipantFromChatRoom(eventId, userId, firstName, lastName);
                return;
            }

            if ("REJECTED".equalsIgnoreCase(status)) {
                log.info("[Kafka Consumer] Первый отказ для юзера {} в событии {}. Пропускаем удаление, давая шанс на повторный запрос.", userId, eventId);
                return;
            }

            log.info("[Kafka Consumer] Запускаем добровольный выход юзера {} из чата события {}, статус: {}", userId, eventId, status);
            groupChatService.leaveGroupRoom(eventId, userId, firstName, lastName);

            log.info("[Kafka Consumer - SUCCESS] Связь с чатом для пользователя {} успешно аннулирована", userId);
        } catch (Exception e) {
            log.error("[Kafka Consumer - ERROR] Ошибка при автоматическом удалении пользователя из чата: {}", e.getMessage(), e);
            throw e;
        }
    }


}
