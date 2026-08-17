package org.example.user.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxAfterCommitListener {

    private final OutboxRelay outboxRelay;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEnqueued(OutboxEnqueuedEvent event) {
        try {
            outboxRelay.tryPublishAfterCommit(event.id());
        } catch (Exception ex) {
            log.warn("[Outbox] Immediate publish failed id={} — poller will retry: {}",
                    event.id(), ex.getMessage());
        }
    }
}
