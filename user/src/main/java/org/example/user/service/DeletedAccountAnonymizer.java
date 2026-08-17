package org.example.user.service;

import com.example.common.kafka.UserEmailUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.user.entity.UserEntity;
import org.example.user.outbox.OutboxService;
import org.example.user.outbox.UserKafkaTopics;
import org.example.user.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeletedAccountAnonymizer {

    private static final int HOLD_DAYS = 7;

    private final UserRepository userRepository;
    private final OutboxService outboxService;

    @Scheduled(fixedDelayString = "${app.account.anonymize-ms:3600000}")
    @Transactional
    public void anonymizeExpired() {
        LocalDateTime before = LocalDateTime.now().minusDays(HOLD_DAYS);
        List<UserEntity> ready = userRepository.findDeletedReadyToAnonymize(before);
        for (UserEntity user : ready) {
            user.setEmail("deleted." + user.getId() + "@deleted.invalid");
            user.setNumberPhone("deleted-" + user.getId());
            user.setAnonymizedAt(LocalDateTime.now());
            userRepository.save(user);
            outboxService.enqueue(
                    UserKafkaTopics.EMAIL_UPDATED,
                    user.getId(),
                    new UserEmailUpdatedEvent(user.getId(), user.getEmail())
            );
            log.info("[User] Освободили email/телефон удалённого аккаунта userId={}", user.getId());
        }
    }
}
