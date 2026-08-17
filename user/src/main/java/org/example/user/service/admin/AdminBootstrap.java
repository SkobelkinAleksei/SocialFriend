package org.example.user.service.admin;

import com.example.common.kafka.UserPlatformRoleChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.user.entity.PlatformRole;
import org.example.user.entity.UserEntity;
import org.example.user.outbox.OutboxService;
import org.example.user.outbox.UserKafkaTopics;
import org.example.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final OutboxService outboxService;

    @Value("${app.admin.bootstrap-user-ids:}")
    private String bootstrapUserIdsRaw = "";

    @Value("${app.admin.bootstrap-emails:}")
    private String bootstrapEmailsRaw = "";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Long> bootstrapUserIds = parseIds(bootstrapUserIdsRaw);
        List<String> bootstrapEmails = parseEmails(bootstrapEmailsRaw);
        if (bootstrapUserIds.isEmpty() && bootstrapEmails.isEmpty()) {
            return;
        }
        List<UserEntity> targets = new ArrayList<>();
        if (bootstrapUserIds != null) {
            for (Long id : bootstrapUserIds) {
                if (id != null) {
                    userRepository.findById(id).ifPresent(targets::add);
                }
            }
        }
        if (bootstrapEmails != null) {
            for (String email : bootstrapEmails) {
                if (email != null && !email.isBlank()) {
                    userRepository.findByEmailIgnoreCase(email.trim()).ifPresent(targets::add);
                }
            }
        }
        for (UserEntity user : targets) {
            if (user.effectivePlatformRole() == PlatformRole.ADMIN) {
                continue;
            }
            user.setPlatformRole(PlatformRole.ADMIN);
            userRepository.save(user);
            outboxService.enqueue(
                    UserKafkaTopics.PLATFORM_ROLE_CHANGED,
                    user.getId(),
                    new UserPlatformRoleChangedEvent(user.getId(), PlatformRole.ADMIN.name())
            );
            log.info("[Admin] Назначена роль ADMIN userId={} email={}", user.getId(), user.getEmail());
        }
    }

    private static List<Long> parseIds(String raw) {
        List<Long> ids = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return ids;
        }
        for (String part : raw.split("[,;\\s]+")) {
            if (part.isBlank()) {
                continue;
            }
            try {
                ids.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {
                // skip
            }
        }
        return ids;
    }

    private static List<String> parseEmails(String raw) {
        List<String> emails = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return emails;
        }
        for (String part : raw.split("[,;\\s]+")) {
            if (!part.isBlank()) {
                emails.add(part.trim());
            }
        }
        return emails;
    }
}
