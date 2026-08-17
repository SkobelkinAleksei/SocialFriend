package org.example.event.outbox;

import com.example.common.metrics.AppMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    static final String AGGREGATE_EVENT = "EVENT";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AppMetrics appMetrics;

    /**
     * Пишет событие в outbox в текущей транзакции. После commit релей отправит в Kafka;
     * если брокер недоступен — догонит поллер.
     */
    @Transactional
    public void enqueue(String topic, Long aggregateId, Object payload) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Outbox enqueue must run inside a database transaction");
        }
        if (aggregateId == null) {
            throw new IllegalArgumentException("Outbox kafka key / aggregate id is required");
        }
        LocalDateTime now = LocalDateTime.now();
        OutboxEvent event = OutboxEvent.builder()
                .topic(topic)
                .kafkaKey(String.valueOf(aggregateId))
                .eventType(payload.getClass().getName())
                .payload(writePayload(payload))
                .aggregateType(AGGREGATE_EVENT)
                .aggregateId(aggregateId)
                .status(OutboxStatus.PENDING)
                .attempts(0)
                .nextAttemptAt(now)
                .createdAt(now)
                .build();
        OutboxEvent saved = outboxEventRepository.saveAndFlush(event);
        eventPublisher.publishEvent(new OutboxEnqueuedEvent(saved.getId()));
        appMetrics.outboxZapisano();
        log.info("[Outbox] Событие записано id={} topic={} aggregateId={}", saved.getId(), topic, aggregateId);
    }

    private String writePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload: " + payload.getClass().getName(), e);
        }
    }
}
