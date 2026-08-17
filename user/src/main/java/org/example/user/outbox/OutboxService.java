package org.example.user.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.common.metrics.AppMetrics;
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
public class OutboxService {

    static final String AGGREGATE_USER = "USER";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AppMetrics appMetrics;

    /**
     * Пишет событие в outbox в текущей транзакции. После commit релей попробует
     * сразу отправить в Kafka; если брокер недоступен — догонит поллер.
     */
    @Transactional
    public void enqueue(String topic, Long userId, Object payload) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Outbox enqueue must run inside a database transaction");
        }
        if (userId == null) {
            throw new IllegalArgumentException("Outbox kafka key / aggregate id is required");
        }
        LocalDateTime now = LocalDateTime.now();
        OutboxEvent event = OutboxEvent.builder()
                .topic(topic)
                .kafkaKey(String.valueOf(userId))
                .eventType(payload.getClass().getName())
                .payload(writePayload(payload))
                .aggregateType(AGGREGATE_USER)
                .aggregateId(userId)
                .status(OutboxStatus.PENDING)
                .attempts(0)
                .nextAttemptAt(now)
                .createdAt(now)
                .build();
        OutboxEvent saved = outboxEventRepository.saveAndFlush(event);
        eventPublisher.publishEvent(new OutboxEnqueuedEvent(saved.getId()));
        appMetrics.outboxZapisano();
        log.info("[Outbox] Событие записано id={} topic={} userId={}", saved.getId(), topic, userId);
    }

    private String writePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload: " + payload.getClass().getName(), e);
        }
    }
}
