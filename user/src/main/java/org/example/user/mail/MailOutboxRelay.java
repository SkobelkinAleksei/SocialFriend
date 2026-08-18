package org.example.user.mail;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.user.service.NeighborhoodMailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailOutboxRelay {

    private final MailOutboxRepository mailOutboxRepository;
    private final NeighborhoodMailService mailService;
    private final TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.mail.outbox.batch-size:20}")
    private int batchSize;

    @Value("${app.mail.outbox.stuck-sending-minutes:2}")
    private int stuckSendingMinutes;

    public void relayDue() {
        List<Long> ids = claimDueIds();
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            trySendClaimed(id);
        }
    }

    public void recoverStuck() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleBefore = now.minusMinutes(stuckSendingMinutes);
        Integer recovered = transactionTemplate.execute(status ->
                mailOutboxRepository.recoverStuckSending(
                        staleBefore, now, MailOutboxStatus.PENDING, MailOutboxStatus.SENDING));
        if (recovered != null && recovered > 0) {
            log.warn("[Почта] Вернули в очередь {} зависших писем", recovered);
        }
    }

    @Async
    public void trySendAfterCommit(Long id) {
        if (id == null) {
            return;
        }
        boolean claimed = Boolean.TRUE.equals(transactionTemplate.execute(status -> claimById(id)));
        if (!claimed) {
            return;
        }
        trySendClaimed(id);
    }

    private List<Long> claimDueIds() {
        return transactionTemplate.execute(status -> {
            @SuppressWarnings("unchecked")
            List<Number> rows = entityManager.createNativeQuery("""
                            WITH due AS (
                                SELECT id FROM mail_outbox
                                WHERE status = 'PENDING' AND next_attempt_at <= :now
                                ORDER BY next_attempt_at, id
                                LIMIT :batch
                                FOR UPDATE SKIP LOCKED
                            )
                            UPDATE mail_outbox e
                            SET status = 'SENDING', locked_at = :now, attempts = attempts + 1
                            FROM due
                            WHERE e.id = due.id
                            RETURNING e.id
                            """)
                    .setParameter("now", LocalDateTime.now())
                    .setParameter("batch", batchSize)
                    .getResultList();
            return rows.stream().map(Number::longValue).toList();
        });
    }

    private boolean claimById(Long id) {
        int updated = entityManager.createNativeQuery("""
                        UPDATE mail_outbox
                        SET status = 'SENDING', locked_at = :now, attempts = attempts + 1
                        WHERE id = :id AND status = 'PENDING'
                        """)
                .setParameter("now", LocalDateTime.now())
                .setParameter("id", id)
                .executeUpdate();
        return updated == 1;
    }

    void trySendClaimed(Long id) {
        MailOutboxEntity letter = mailOutboxRepository.findById(id).orElse(null);
        if (letter == null || letter.getStatus() != MailOutboxStatus.SENDING) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (letter.getExpiresAt() != null && !letter.getExpiresAt().isAfter(now)) {
            transactionTemplate.executeWithoutResult(status -> markDead(id, "Код уже не действует"));
            log.warn("[Почта] Письмо id={} просрочено, не отправляем", id);
            return;
        }
        try {
            mailService.sendCode(letter.getToEmail(), letter.getSubject(), letter.getBody(), "");
            transactionTemplate.executeWithoutResult(status -> markSent(id));
        } catch (Exception ex) {
            log.warn("[Почта] Не удалось отправить id={} на {}: {}", id, letter.getToEmail(), ex.getMessage());
            transactionTemplate.executeWithoutResult(status -> markFailure(id, ex));
        }
    }

    private void markSent(Long id) {
        MailOutboxEntity current = mailOutboxRepository.findById(id).orElse(null);
        if (current == null) {
            return;
        }
        current.setStatus(MailOutboxStatus.SENT);
        current.setLockedAt(null);
        current.setLastError(null);
        current.setSentAt(LocalDateTime.now());
        current.setBody("");
        mailOutboxRepository.save(current);
    }

    private void markDead(Long id, String reason) {
        MailOutboxEntity current = mailOutboxRepository.findById(id).orElse(null);
        if (current == null) {
            return;
        }
        current.setStatus(MailOutboxStatus.DEAD);
        current.setLockedAt(null);
        current.setLastError(reason);
        current.setBody("");
        mailOutboxRepository.save(current);
    }

    private void markFailure(Long id, Exception ex) {
        MailOutboxEntity current = mailOutboxRepository.findById(id).orElse(null);
        if (current == null) {
            return;
        }
        current.setLockedAt(null);
        current.setLastError(trimError(ex));
        current.setStatus(MailOutboxStatus.PENDING);
        current.setNextAttemptAt(nextAttemptAt(current.getAttempts()));
        mailOutboxRepository.save(current);
    }

    private static LocalDateTime nextAttemptAt(int attempts) {
        long seconds = Math.min(60L, 1L << Math.min(Math.max(attempts, 1), 6));
        return LocalDateTime.now().plusSeconds(seconds);
    }

    private static String trimError(Exception ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }
}
