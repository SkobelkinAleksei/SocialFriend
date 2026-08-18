package org.example.user.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailOutboxService {

    private final MailOutboxRepository mailOutboxRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long enqueue(String toEmail, String subject, String body, LocalDateTime expiresAt) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Mail outbox enqueue must run inside a database transaction");
        }
        LocalDateTime now = LocalDateTime.now();
        MailOutboxEntity saved = mailOutboxRepository.saveAndFlush(MailOutboxEntity.builder()
                .toEmail(toEmail)
                .subject(subject)
                .body(body)
                .status(MailOutboxStatus.PENDING)
                .attempts(0)
                .nextAttemptAt(now)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build());
        eventPublisher.publishEvent(new MailOutboxEnqueuedEvent(saved.getId()));
        log.info("[Почта] Письмо поставлено в очередь id={} на {}", saved.getId(), toEmail);
        return saved.getId();
    }
}
