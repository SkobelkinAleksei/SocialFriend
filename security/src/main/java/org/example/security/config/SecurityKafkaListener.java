package org.example.security.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eventskafka.user.UserEmailUpdatedEvent;
import org.example.eventskafka.user.UserPasswordUpdatedEvent;
import org.example.eventskafka.user.UserRegisteredEvent;
import org.example.security.service.SecurityUserService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityKafkaListener {

    private final SecurityUserService userSecurityService;

    @KafkaListener(
            topics = "user-registered",
            groupId = "security-group"
    )
    public void listenUserRegistered(UserRegisteredEvent event) {
        log.info("[SecurityKafkaListener - INFO] Получено событие создания пользователя: {}", event.getUsername());
        userSecurityService.createUserFromEvent(event);
    }

    // Обновление почты
    @KafkaListener(
            topics = "user-email-updated",
            groupId = "security-group"
    )
    public void listenEmailUpdated(UserEmailUpdatedEvent event) {
        log.info("[SecurityKafkaListener - INFO] Получено событие обновления почты для пользователя: {}", event.getUserId());
        userSecurityService.updateEmail(event.getUserId(), event.getNewEmail());
    }

    // Обновление пароля
    @KafkaListener(
            topics = "user-password-updated",
            groupId = "security-group"
    )
    public void listenPasswordUpdated(UserPasswordUpdatedEvent event) {
        log.info("[SecurityKafkaListener - INFO] Получено событие обновления пароля для пользователя: {}", event.getUserId());
        userSecurityService.updatePassword(event.getUserId(), event.getNewPassword());
    }
}