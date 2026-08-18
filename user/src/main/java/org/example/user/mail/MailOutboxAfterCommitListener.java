package org.example.user.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailOutboxAfterCommitListener {

    private final MailOutboxRelay mailOutboxRelay;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEnqueued(MailOutboxEnqueuedEvent event) {
        try {
            mailOutboxRelay.trySendAfterCommit(event.id());
        } catch (Exception ex) {
            log.warn("[Почта] Сразу после сохранения не отправили id={} — поллер повторит: {}",
                    event.id(), ex.getMessage());
        }
    }
}
