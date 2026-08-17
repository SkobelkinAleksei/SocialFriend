package org.example.user.outbox;

import com.example.common.kafka.UserAccountStatusChangedEvent;
import com.example.common.kafka.UserEmailUpdatedEvent;
import com.example.common.kafka.UserPasswordUpdatedEvent;
import com.example.common.kafka.UserPlatformRoleChangedEvent;
import com.example.common.kafka.UserRegisteredEvent;
import com.example.common.kafka.UserSettingsUpdatedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.example.common.metrics.AppMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxRelay {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            UserRegisteredEvent.class.getName(),
            UserEmailUpdatedEvent.class.getName(),
            UserPasswordUpdatedEvent.class.getName(),
            UserSettingsUpdatedEvent.class.getName(),
            UserAccountStatusChangedEvent.class.getName(),
            UserPlatformRoleChangedEvent.class.getName()
    );

    private static final Map<String, Class<?>> TYPE_BY_NAME = Map.of(
            UserRegisteredEvent.class.getName(), UserRegisteredEvent.class,
            UserEmailUpdatedEvent.class.getName(), UserEmailUpdatedEvent.class,
            UserPasswordUpdatedEvent.class.getName(), UserPasswordUpdatedEvent.class,
            UserSettingsUpdatedEvent.class.getName(), UserSettingsUpdatedEvent.class,
            UserAccountStatusChangedEvent.class.getName(), UserAccountStatusChangedEvent.class,
            UserPlatformRoleChangedEvent.class.getName(), UserPlatformRoleChangedEvent.class
    );

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final AppMetrics appMetrics;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.outbox.batch-size:20}")
    private int batchSize;

    @Value("${app.outbox.publish-timeout-seconds:5}")
    private int publishTimeoutSeconds;

    @Value("${app.outbox.stuck-sending-minutes:2}")
    private int stuckSendingMinutes;

    public void relayDue() {
        List<Long> ids = claimDueIds();
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (int i = 0; i < ids.size(); i++) {
            boolean published = tryPublishClaimed(ids.get(i));
            if (!published) {
                revertUnprocessed(ids.subList(i + 1, ids.size()));
                return;
            }
        }
    }

    public void recoverStuck() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime staleBefore = now.minusMinutes(stuckSendingMinutes);
        Integer recovered = transactionTemplate.execute(status ->
                outboxEventRepository.recoverStuckSending(
                        staleBefore, now, OutboxStatus.PENDING, OutboxStatus.SENDING));
        if (recovered != null && recovered > 0) {
            log.warn("[Outbox] Вернули в очередь {} зависших событий", recovered);
        }
    }

    /**
     * Сразу после commit бизнес-транзакции (асинхронно, чтобы API не ждал Kafka).
     * Если строку уже забрал поллер — no-op.
     */
    @Async
    public void tryPublishAfterCommit(Long id) {
        if (id == null) {
            return;
        }
        boolean claimed = Boolean.TRUE.equals(transactionTemplate.execute(status -> claimById(id)));
        if (!claimed) {
            return;
        }
        tryPublishClaimed(id);
    }

    private List<Long> claimDueIds() {
        return transactionTemplate.execute(status -> {
            @SuppressWarnings("unchecked")
            List<Number> rows = entityManager.createNativeQuery("""
                            WITH due AS (
                                SELECT id FROM outbox_events
                                WHERE status = 'PENDING' AND next_attempt_at <= :now
                                ORDER BY next_attempt_at, id
                                LIMIT :batch
                                FOR UPDATE SKIP LOCKED
                            )
                            UPDATE outbox_events e
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
                        UPDATE outbox_events
                        SET status = 'SENDING', locked_at = :now, attempts = attempts + 1
                        WHERE id = :id AND status = 'PENDING'
                        """)
                .setParameter("now", LocalDateTime.now())
                .setParameter("id", id)
                .executeUpdate();
        return updated == 1;
    }

    private boolean tryPublishClaimed(Long id) {
        OutboxEvent event = outboxEventRepository.findById(id).orElse(null);
        if (event == null || event.getStatus() != OutboxStatus.SENDING) {
            return true;
        }
        try {
            Object payload = deserialize(event);
            kafkaTemplate.send(event.getTopic(), event.getKafkaKey(), payload)
                    .get(publishTimeoutSeconds, TimeUnit.SECONDS);
            transactionTemplate.executeWithoutResult(status -> markSent(id));
            appMetrics.outboxOtpravleno();
            log.info("[Outbox] Отправлено id={} topic={} key={}", id, event.getTopic(), event.getKafkaKey());
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("[Outbox] Прервана отправка id={} topic={}", id, event.getTopic());
            transactionTemplate.executeWithoutResult(status -> markFailure(id, ex));
            appMetrics.outboxOshibka();
            return false;
        } catch (Exception ex) {
            log.warn("[Outbox] Не удалось отправить id={} topic={}: {}", id, event.getTopic(), ex.getMessage());
            transactionTemplate.executeWithoutResult(status -> markFailure(id, ex));
            appMetrics.outboxOshibka();
            return isPoison(ex);
        }
    }

    private void revertUnprocessed(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            LocalDateTime now = LocalDateTime.now();
            for (Long id : ids) {
                OutboxEvent current = outboxEventRepository.findById(id).orElse(null);
                if (current == null || current.getStatus() != OutboxStatus.SENDING) {
                    continue;
                }
                current.setStatus(OutboxStatus.PENDING);
                current.setLockedAt(null);
                current.setAttempts(Math.max(0, current.getAttempts() - 1));
                current.setNextAttemptAt(now);
                outboxEventRepository.save(current);
            }
        });
    }

    private void markSent(Long id) {
        OutboxEvent current = outboxEventRepository.findById(id).orElse(null);
        if (current == null) {
            return;
        }
        current.setStatus(OutboxStatus.SENT);
        current.setLockedAt(null);
        current.setLastError(null);
        current.setSentAt(LocalDateTime.now());
        outboxEventRepository.save(current);
    }

    private void markFailure(Long id, Exception ex) {
        OutboxEvent current = outboxEventRepository.findById(id).orElse(null);
        if (current == null) {
            return;
        }
        current.setLockedAt(null);
        current.setLastError(trimError(ex));
        if (isPoison(ex)) {
            current.setStatus(OutboxStatus.DEAD);
            log.error("[Outbox] Poison payload id={} type={} — помечен DEAD", id, current.getEventType(), ex);
        } else {
            current.setStatus(OutboxStatus.PENDING);
            current.setNextAttemptAt(nextAttemptAt(current.getAttempts()));
        }
        outboxEventRepository.save(current);
    }

    private Object deserialize(OutboxEvent event) throws JsonProcessingException {
        if (!ALLOWED_TYPES.contains(event.getEventType())) {
            throw new IllegalArgumentException("Unknown outbox event type: " + event.getEventType());
        }
        Class<?> type = TYPE_BY_NAME.get(event.getEventType());
        return objectMapper.readValue(event.getPayload(), type);
    }

    private static LocalDateTime nextAttemptAt(int attempts) {
        long seconds = Math.min(300L, 1L << Math.min(Math.max(attempts, 1), 8));
        return LocalDateTime.now().plusSeconds(seconds);
    }

    private static boolean isPoison(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof JsonProcessingException
                    || current instanceof ClassNotFoundException
                    || current instanceof IllegalArgumentException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String trimError(Exception ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return message.length() > 2000 ? message.substring(0, 2000) : message;
    }
}
