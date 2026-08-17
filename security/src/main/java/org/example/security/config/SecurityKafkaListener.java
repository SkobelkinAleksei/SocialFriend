package org.example.security.config;

import com.example.common.kafka.UserAccountStatusChangedEvent;
import com.example.common.kafka.UserEmailUpdatedEvent;
import com.example.common.kafka.UserPasswordUpdatedEvent;
import com.example.common.kafka.UserPlatformRoleChangedEvent;
import com.example.common.kafka.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.security.service.RefreshTokenService;
import org.example.security.service.SecurityUserService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityKafkaListener {

    private final SecurityUserService userSecurityService;
    private final RefreshTokenService refreshTokenService;

    @KafkaListener(
            topics = "user-registered",
            groupId = "security-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listenUserRegistered(UserRegisteredEvent event) {
        log.info("[SecurityKafkaListener] Регистрация userId={}, email={}", event.getId(), event.getEmail());
        userSecurityService.createUserFromEvent(event);
    }

    @KafkaListener(
            topics = "user-email-updated",
            groupId = "security-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listenEmailUpdated(UserEmailUpdatedEvent event) {
        log.info("[SecurityKafkaListener] Обновление email для userId={}", event.getUserId());
        userSecurityService.updateEmail(event.getUserId(), event.getNewEmail());
        refreshTokenService.revokeAllForUser(event.getUserId());
    }

    @KafkaListener(
            topics = "user-password-updated",
            groupId = "security-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listenPasswordUpdated(UserPasswordUpdatedEvent event) {
        log.info("[SecurityKafkaListener] Обновление пароля для userId={}", event.getUserId());
        userSecurityService.updatePassword(event.getUserId(), event.getPasswordHash());
        refreshTokenService.revokeAllForUser(event.getUserId());
    }

    @KafkaListener(
            topics = "user-account-status-changed",
            groupId = "security-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listenAccountStatusChanged(UserAccountStatusChangedEvent event) {
        log.info("[SecurityKafkaListener] Статус аккаунта userId={} → {}", event.getUserId(), event.getStatus());
        userSecurityService.applyAccountStatus(event.getUserId(), event.getStatus());
        if (!"ACTIVE".equalsIgnoreCase(event.getStatus())) {
            refreshTokenService.revokeAllForUser(event.getUserId());
        }
    }

    @KafkaListener(
            topics = "user-platform-role-changed",
            groupId = "security-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void listenPlatformRoleChanged(UserPlatformRoleChangedEvent event) {
        log.info("[SecurityKafkaListener] Роль userId={} → {}", event.getUserId(), event.getRole());
        userSecurityService.applyPlatformRole(event.getUserId(), event.getRole());
        refreshTokenService.revokeAllForUser(event.getUserId());
    }
}
